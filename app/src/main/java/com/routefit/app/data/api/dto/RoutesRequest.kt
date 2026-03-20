package com.routefit.app.data.api.dto

data class ComputeRoutesRequest(
    val origin: WaypointDto,
    val destination: WaypointDto,
    val intermediates: List<WaypointDto>? = null,
    val travelMode: String = "WALK",
    val computeAlternativeRoutes: Boolean = false
)
// routeModifiers is intentionally omitted: avoidFerries/avoidHighways/avoidTolls are all
// invalid for WALK travel mode in the Routes API v2 and cause a 400 validation error.

data class WaypointDto(
    val location: LocationWrapperDto
)

data class LocationWrapperDto(
    val latLng: LatLngDto
)

data class LatLngDto(
    val latitude: Double,
    val longitude: Double
)
