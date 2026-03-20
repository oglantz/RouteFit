package com.routefit.app.data.api.dto

data class ComputeRoutesRequest(
    val origin: WaypointDto,
    val destination: WaypointDto,
    val intermediates: List<WaypointDto>? = null,
    val travelMode: String = "WALK",
    val computeAlternativeRoutes: Boolean = false,
    val routeModifiers: RouteModifiersDto = RouteModifiersDto()
)

data class RouteModifiersDto(
    val avoidFerries: Boolean = true,
    val avoidHighways: Boolean = false,
    val avoidTolls: Boolean = false
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
