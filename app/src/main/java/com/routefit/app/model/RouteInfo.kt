package com.routefit.app.model

import com.google.android.gms.maps.model.LatLng

data class RouteInfo(
    val distanceMeters: Int,
    val durationSeconds: Int,
    val polylinePoints: List<LatLng>,
    val waypoints: List<AppLocation> = emptyList(),
    val score: Double = 0.0
)
