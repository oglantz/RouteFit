package com.routefit.app.domain

import com.routefit.app.model.AppLocation
import com.routefit.app.model.RouteInfo
import kotlin.math.PI
import kotlin.math.abs
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
        targetDistanceMeters: Double,
        toleranceMeters: Double = 402.336
    ): List<RouteInfo> {
        val results = mutableListOf<RouteInfo>()

        val baseline = routeProvider.getWalkingRoute(start, end) ?: return emptyList()

        if (abs(baseline.distanceMeters - targetDistanceMeters) <= toleranceMeters) {
            results.add(baseline)
        }

        val isLoop = isLoopRoute(start, end)
        val waypointSets = if (isLoop) {
            generateLoopWaypoints(start, targetDistanceMeters)
        } else {
            generateAtoBWaypoints(start, end, targetDistanceMeters, baseline.distanceMeters.toDouble())
        }

        for (wpSet in waypointSets) {
            try {
                val route = routeProvider.getWalkingRoute(start, end, wpSet) ?: continue
                results.add(route)
            } catch (_: Exception) {
                continue
            }
        }

        val scorer = RouteScorer()
        val scored = scorer.scoreRoutes(results, targetDistanceMeters, start, end)
        return scored.take(3)
    }

    private fun isLoopRoute(start: AppLocation, end: AppLocation): Boolean {
        return haversineDistance(start, end) < 100
    }

    private fun generateLoopWaypoints(
        center: AppLocation,
        targetDistanceMeters: Double
    ): List<List<AppLocation>> {
        val candidates = mutableListOf<List<AppLocation>>()
        val baseRadius = targetDistanceMeters / (2 * PI)
        val radii = listOf(
            baseRadius * 0.7, baseRadius * 0.85, baseRadius,
            baseRadius * 1.15, baseRadius * 1.3
        )

        for (radius in radii) {
            for (angle in listOf(0.0, 45.0, 90.0, 135.0, 180.0, 225.0, 270.0, 315.0)) {
                candidates.add(listOf(offsetPoint(center, radius, angle)))
            }
            for (angle in listOf(0.0, 45.0, 90.0, 135.0)) {
                val wp1 = offsetPoint(center, radius, angle)
                val wp2 = offsetPoint(center, radius, angle + 180.0)
                candidates.add(listOf(wp1, wp2))
            }
        }

        return candidates
    }

    private fun generateAtoBWaypoints(
        start: AppLocation,
        end: AppLocation,
        targetDistanceMeters: Double,
        baselineDistanceMeters: Double
    ): List<List<AppLocation>> {
        val candidates = mutableListOf<List<AppLocation>>()
        val midLat = (start.latitude + end.latitude) / 2
        val midLng = (start.longitude + end.longitude) / 2
        val midpoint = AppLocation(midLat, midLng)

        val extraDistance = targetDistanceMeters - baselineDistanceMeters
        val dx = end.longitude - start.longitude
        val dy = end.latitude - start.latitude
        val perpAngle = Math.toDegrees(atan2(dx, -dy))

        val offsets = if (extraDistance > 0) {
            val baseOffset = extraDistance / 4
            listOf(baseOffset * 0.5, baseOffset, baseOffset * 1.5, baseOffset * 2.0)
        } else {
            val directDistance = haversineDistance(start, end)
            listOf(directDistance * 0.05, directDistance * 0.1, directDistance * 0.15)
        }

        for (offset in offsets) {
            candidates.add(listOf(offsetPoint(midpoint, offset, perpAngle)))
            candidates.add(listOf(offsetPoint(midpoint, offset, perpAngle + 180.0)))
        }

        if (extraDistance > 0) {
            val offset = extraDistance / 6
            val q1 = AppLocation(
                start.latitude + (end.latitude - start.latitude) * 0.25,
                start.longitude + (end.longitude - start.longitude) * 0.25
            )
            val q2 = AppLocation(
                start.latitude + (end.latitude - start.latitude) * 0.75,
                start.longitude + (end.longitude - start.longitude) * 0.75
            )
            for (off in listOf(offset, offset * 1.5, offset * 2.0)) {
                candidates.add(
                    listOf(offsetPoint(q1, off, perpAngle), offsetPoint(q2, off, perpAngle))
                )
                candidates.add(
                    listOf(
                        offsetPoint(q1, off, perpAngle + 180.0),
                        offsetPoint(q2, off, perpAngle + 180.0)
                    )
                )
            }
        }

        return candidates
    }

    companion object {

        fun offsetPoint(
            center: AppLocation,
            distanceMeters: Double,
            bearingDegrees: Double
        ): AppLocation {
            val R = 6371000.0
            val lat1 = Math.toRadians(center.latitude)
            val lng1 = Math.toRadians(center.longitude)
            val bearing = Math.toRadians(bearingDegrees)
            val d = distanceMeters / R

            val lat2 = asin(
                sin(lat1) * cos(d) + cos(lat1) * sin(d) * cos(bearing)
            )
            val lng2 = lng1 + atan2(
                sin(bearing) * sin(d) * cos(lat1),
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
