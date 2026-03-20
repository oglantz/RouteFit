package com.routefit.app.domain

import com.google.android.gms.maps.model.LatLng
import com.routefit.app.model.AppLocation
import com.routefit.app.model.RouteInfo
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class RouteGenerator(private val routeProvider: RouteProvider) {

    interface RouteProvider {
        suspend fun getWalkingRoute(
            origin: AppLocation,
            destination: AppLocation,
            waypoints: List<AppLocation> = emptyList()
        ): RouteInfo?
    }

    suspend fun generateRoutes(
        start: AppLocation,
        end: AppLocation,
        targetDistanceMeters: Double
    ): List<RouteInfo> {
        val baseline = routeProvider.getWalkingRoute(start, end) ?: return emptyList()
        val results = mutableListOf<RouteInfo>()
        if (isLikelyWalkOnly(baseline)) {
            results.add(baseline)
        }

        val isLoop = isLoopRoute(start, end)
        val waypointSets = if (isLoop) {
            generateLoopWaypoints(start, targetDistanceMeters)
        } else {
            generateAtoBWaypointsFromBaseline(baseline, targetDistanceMeters)
        }

        for (wpSet in waypointSets) {
            try {
                val route = routeProvider.getWalkingRoute(start, end, wpSet) ?: continue
                if (isLikelyWalkOnly(route)) {
                    results.add(route)
                }
            } catch (_: Exception) {
                continue
            }
        }

        val seen = mutableSetOf<Int>()
        val unique = results.filter { seen.add(it.distanceMeters) }

        val scorer = RouteScorer()
        val scored = scorer.scoreRoutes(unique, targetDistanceMeters, start, end)
        return scored
            .sortedBy { kotlin.math.abs(it.distanceMeters - targetDistanceMeters) }
            .take(3)
    }

    private fun isLoopRoute(start: AppLocation, end: AppLocation): Boolean {
        return haversineDistance(start, end) < 100
    }

    // ---- Polyline sampling ----

    private fun samplePolylinePoints(
        polyline: List<AppLocation>,
        fractions: List<Double>
    ): List<Pair<AppLocation, Double>> {
        if (polyline.size < 2) return emptyList()

        val segDistances = mutableListOf<Double>()
        var totalDist = 0.0
        for (i in 0 until polyline.size - 1) {
            val d = haversineDistance(polyline[i], polyline[i + 1])
            segDistances.add(d)
            totalDist += d
        }
        if (totalDist == 0.0) return emptyList()

        return fractions.mapNotNull { frac ->
            val targetDist = frac * totalDist
            var accumulated = 0.0
            for (i in segDistances.indices) {
                val segLen = segDistances[i]
                if (accumulated + segLen >= targetDist) {
                    val ratio = if (segLen > 0) (targetDist - accumulated) / segLen else 0.0
                    val lat = polyline[i].latitude +
                            ratio * (polyline[i + 1].latitude - polyline[i].latitude)
                    val lng = polyline[i].longitude +
                            ratio * (polyline[i + 1].longitude - polyline[i].longitude)
                    val localBearing = bearing(polyline[i], polyline[i + 1])
                    return@mapNotNull Pair(AppLocation(lat, lng), localBearing)
                }
                accumulated += segLen
            }
            null
        }
    }

    // ---- A-to-B: polyline-anchored, inland-only waypoints ----

    private fun generateAtoBWaypointsFromBaseline(
        baseline: RouteInfo,
        targetDistanceMeters: Double
    ): List<List<AppLocation>> {
        val polyline: List<AppLocation> = baseline.polylinePoints.map { point: LatLng ->
            AppLocation(point.latitude, point.longitude)
        }
        if (polyline.size < 2) return emptyList()

        val baselineDist = baseline.distanceMeters.toDouble()
        val extraDistance = (targetDistanceMeters - baselineDist).coerceAtLeast(0.0)
        val candidates = mutableListOf<List<AppLocation>>()

        // Centroid of baseline polyline — the "center of mass" of the route,
        // guaranteed to be over land. We only offset waypoints TOWARD this
        // centroid to avoid sending routes across water.
        var latSum = 0.0
        var lngSum = 0.0
        for (point in polyline) {
            latSum += point.latitude
            lngSum += point.longitude
        }
        val centroidLat = latSum / polyline.size
        val centroidLng = lngSum / polyline.size
        val centroid = AppLocation(centroidLat, centroidLng)

        val maxOffset = if (extraDistance > 0) {
            (extraDistance / 3.0).coerceIn(300.0, 8000.0)
        } else {
            (baselineDist * 0.1).coerceIn(200.0, 1500.0)
        }

        val offsets = listOf(
            maxOffset * 0.2,
            maxOffset * 0.35,
            maxOffset * 0.5,
            maxOffset * 0.7,
            maxOffset * 0.9,
            maxOffset * 1.1
        )

        // Sample 5 anchor points along the baseline
        val fracs = listOf(0.2, 0.33, 0.5, 0.67, 0.8)
        val sampled = samplePolylinePoints(polyline, fracs)
        if (sampled.isEmpty()) return candidates

        // For each anchor, determine the "inland" perpendicular direction
        // (the one closer to the polyline centroid)
        val anchorPoints = mutableListOf<AppLocation>()
        val inlandBearings = mutableListOf<Double>()

        for ((pt, routeBearing) in sampled) {
            val perpLeft = routeBearing + 90.0
            val perpRight = routeBearing - 90.0
            val testLeft = offsetPoint(pt, 100.0, perpLeft)
            val testRight = offsetPoint(pt, 100.0, perpRight)
            val inland = if (haversineDistance(testLeft, centroid) <
                haversineDistance(testRight, centroid)
            ) perpLeft else perpRight

            anchorPoints.add(pt)
            inlandBearings.add(inland)
        }

        // --- Single waypoint at 0.33, 0.5, 0.67 (inland only) ---
        val singleIndices = listOf(1, 2, 3).filter { it < anchorPoints.size }
        for (idx in singleIndices) {
            for (off in offsets) {
                candidates.add(
                    listOf(offsetPoint(anchorPoints[idx], off, inlandBearings[idx]))
                )
            }
        }

        // --- Two waypoints at 0.33 + 0.67 (both inland) ---
        if (anchorPoints.size >= 4) {
            for (off in offsets) {
                candidates.add(listOf(
                    offsetPoint(anchorPoints[1], off, inlandBearings[1]),
                    offsetPoint(anchorPoints[3], off, inlandBearings[3])
                ))
                candidates.add(listOf(
                    offsetPoint(anchorPoints[1], off * 0.5, inlandBearings[1]),
                    offsetPoint(anchorPoints[3], off, inlandBearings[3])
                ))
            }
        }

        // --- Three waypoints at 0.2, 0.5, 0.8 for medium+ gaps ---
        if (extraDistance > baselineDist * 0.3 && anchorPoints.size >= 5) {
            for (off in offsets) {
                candidates.add(listOf(
                    offsetPoint(anchorPoints[0], off * 0.7, inlandBearings[0]),
                    offsetPoint(anchorPoints[2], off, inlandBearings[2]),
                    offsetPoint(anchorPoints[4], off * 0.7, inlandBearings[4])
                ))
            }
        }

        // --- Four waypoints at 0.2, 0.33, 0.67, 0.8 for large gaps ---
        if (extraDistance > baselineDist && anchorPoints.size >= 5) {
            for (off in offsets) {
                candidates.add(listOf(
                    offsetPoint(anchorPoints[0], off, inlandBearings[0]),
                    offsetPoint(anchorPoints[1], off * 0.8, inlandBearings[1]),
                    offsetPoint(anchorPoints[3], off * 0.8, inlandBearings[3]),
                    offsetPoint(anchorPoints[4], off, inlandBearings[4])
                ))
            }
        }

        return candidates
    }

    private fun isLikelyWalkOnly(route: RouteInfo): Boolean {
        if (route.durationSeconds <= 0 || route.distanceMeters <= 0) return false
        val avgSpeedMps = route.distanceMeters.toDouble() / route.durationSeconds.toDouble()
        // Walking speeds outside this range are usually ferries or invalid segments.
        return avgSpeedMps in 0.5..2.0
    }

    // ---- Loop routes: geometric (start ≈ end, no useful baseline) ----

    private fun generateLoopWaypoints(
        center: AppLocation,
        targetDistanceMeters: Double
    ): List<List<AppLocation>> {
        val candidates = mutableListOf<List<AppLocation>>()
        val baseRadius = targetDistanceMeters / (2 * PI)
        val radii = listOf(baseRadius * 0.7, baseRadius, baseRadius * 1.3)

        for (radius in radii) {
            for (angle in listOf(0.0, 45.0, 90.0, 135.0, 180.0, 225.0, 270.0, 315.0)) {
                candidates.add(listOf(offsetPoint(center, radius, angle)))
            }
            for (angle in listOf(0.0, 90.0, 180.0, 270.0)) {
                val wp1 = offsetPoint(center, radius, angle)
                val wp2 = offsetPoint(center, radius, angle + 180.0)
                candidates.add(listOf(wp1, wp2))
            }
        }

        return candidates
    }

    companion object {

        fun bearing(from: AppLocation, to: AppLocation): Double {
            val lat1 = Math.toRadians(from.latitude)
            val lat2 = Math.toRadians(to.latitude)
            val dLng = Math.toRadians(to.longitude - from.longitude)
            val y = sin(dLng) * cos(lat2)
            val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLng)
            return Math.toDegrees(atan2(y, x))
        }

        fun offsetPoint(
            center: AppLocation,
            distanceMeters: Double,
            bearingDegrees: Double
        ): AppLocation {
            val R = 6371000.0
            val lat1 = Math.toRadians(center.latitude)
            val lng1 = Math.toRadians(center.longitude)
            val brng = Math.toRadians(bearingDegrees)
            val d = distanceMeters / R

            val lat2 = asin(
                sin(lat1) * cos(d) + cos(lat1) * sin(d) * cos(brng)
            )
            val lng2 = lng1 + atan2(
                sin(brng) * sin(d) * cos(lat1),
                cos(d) - sin(lat1) * sin(lat2)
            )

            return AppLocation(Math.toDegrees(lat2), Math.toDegrees(lng2))
        }

        fun haversineDistance(a: AppLocation, b: AppLocation): Double {
            val R = 6371000.0
            val dLat = Math.toRadians(b.latitude - a.latitude)
            val dLng = Math.toRadians(b.longitude - a.longitude)
            val sinDLat = sin(dLat / 2)
            val sinDLng = sin(dLng / 2)
            val a1 = sinDLat * sinDLat +
                    cos(Math.toRadians(a.latitude)) * cos(Math.toRadians(b.latitude)) *
                    sinDLng * sinDLng
            val c = 2 * atan2(sqrt(a1), sqrt(1 - a1))
            return R * c
        }
    }
}
