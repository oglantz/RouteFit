package com.routefit.app.domain

import com.routefit.app.model.AppLocation
import com.routefit.app.model.RouteInfo
import kotlin.math.abs
import kotlin.math.min

class RouteScorer {

    fun scoreRoutes(
        routes: List<RouteInfo>,
        targetDistanceMeters: Double,
        start: AppLocation,
        end: AppLocation
    ): List<RouteInfo> {
        if (routes.isEmpty()) return emptyList()

        return routes.map { route ->
            val score = calculateScore(route, targetDistanceMeters, start, end)
            route.copy(score = score)
        }.sortedWith(
            compareBy<RouteInfo> {
                abs(it.distanceMeters.toDouble() - targetDistanceMeters)
            }.thenByDescending { it.score }
        )
    }

    private fun calculateScore(
        route: RouteInfo,
        targetDistanceMeters: Double,
        start: AppLocation,
        end: AppLocation
    ): Double {
        val distanceDelta = abs(route.distanceMeters.toDouble() - targetDistanceMeters)
        val distanceScore =
            (1.0 - min(1.0, distanceDelta / (0.2 * targetDistanceMeters))).coerceAtLeast(0.0)

        val directDistance = RouteGenerator.haversineDistance(start, end)
        val simplicityScore = if (route.distanceMeters > 0 && directDistance > 0) {
            min(1.0, directDistance / route.distanceMeters)
        } else {
            0.5
        }

        // Prioritize target matching first; simplicity is only a tie-breaker.
        return 0.95 * distanceScore + 0.05 * simplicityScore
    }
}
