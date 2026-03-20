package com.routefit.app.data.api.dto

data class ComputeRoutesRequest(
    val origin: WaypointDto,
    val destination: WaypointDto,
    val intermediates: List<WaypointDto>? = null,
    val travelMode: String = "WALK",
    val computeAlternativeRoutes: Boolean = false,
    val routeModifiers: RouteModifiersDto = RouteModifiersDto()
)

// avoidHighways and avoidTolls are not valid for WALK travel mode and will cause
// a validation error from the Routes API even when set to false — omit them entirely.
data class RouteModifiersDto(
    val avoidFerries: Boolean = true
)

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
