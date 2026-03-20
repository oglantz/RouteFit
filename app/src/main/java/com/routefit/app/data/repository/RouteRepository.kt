package com.routefit.app.data.repository

import com.routefit.app.data.api.RoutesApiService
import com.routefit.app.data.api.dto.ComputeRoutesRequest
import com.routefit.app.data.api.dto.LatLngDto
import com.routefit.app.data.api.dto.LocationWrapperDto
import com.routefit.app.data.api.dto.WaypointDto
import com.routefit.app.model.AppLocation
import com.routefit.app.model.RouteInfo
import com.routefit.app.utils.DistanceUtils
import com.routefit.app.utils.PolylineDecoder

class RouteRepository(
    private val apiService: RoutesApiService,
    private val apiKey: String
) {

    suspend fun computeWalkingRoute(
        origin: AppLocation,
        destination: AppLocation,
        waypoints: List<AppLocation> = emptyList()
    ): RouteInfo? {
        val request = ComputeRoutesRequest(
            origin = origin.toWaypointDto(),
            destination = destination.toWaypointDto(),
            intermediates = waypoints.takeIf { it.isNotEmpty() }?.map { it.toWaypointDto() }
        )

        return try {
            val response = apiService.computeRoutes(apiKey, request = request)
            val route = response.routes?.firstOrNull() ?: return null

            RouteInfo(
                distanceMeters = route.distanceMeters ?: 0,
                durationSeconds = DistanceUtils.parseDuration(route.duration ?: "0s"),
                polylinePoints = PolylineDecoder.decode(route.polyline?.encodedPolyline ?: ""),
                waypoints = waypoints
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun AppLocation.toWaypointDto() = WaypointDto(
        location = LocationWrapperDto(
            latLng = LatLngDto(latitude, longitude)
        )
    )
}
