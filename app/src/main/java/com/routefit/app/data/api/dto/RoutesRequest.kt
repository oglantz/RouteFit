package com.routefit.app.data.api.dto

data class ComputeRoutesRequest(
    val origin: WaypointDto,
    val destination: WaypointDto,
    val intermediates: List<WaypointDto>? = null,
    val travelMode: String = "WALK",
    val computeAlternativeRoutes: Boolean = false,
    // Higher detail prevents visual shortcuts that can appear over water.
    val polylineQuality: String = "HIGH_QUALITY"
)
// routeModifiers is intentionally omitted: avoidFerries/avoidHighways/avoidTolls are all
// invalid for WALK travel mode in the Routes API v2 and cause a 400 validation error.

data class WaypointDto(
    val location: LocationWrapperDto,
    // via=true makes this a pass-through waypoint (not a stop); the route must
    // pass near it but the router has more flexibility in how it approaches it.
    val via: Boolean = false
)

data class LocationWrapperDto(
    val latLng: LatLngDto
)

data class LatLngDto(
    val latitude: Double,
    val longitude: Double
)
