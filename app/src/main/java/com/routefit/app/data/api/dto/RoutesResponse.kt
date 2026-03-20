package com.routefit.app.data.api.dto

data class ComputeRoutesResponse(
    val routes: List<RouteDto>? = null
)

data class RouteDto(
    val distanceMeters: Int? = null,
    val duration: String? = null,
    val polyline: PolylineDto? = null
)

data class PolylineDto(
    val encodedPolyline: String? = null
)
