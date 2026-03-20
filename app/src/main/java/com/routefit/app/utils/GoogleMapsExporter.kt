package com.routefit.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.routefit.app.model.AppLocation

object GoogleMapsExporter {

    fun buildGoogleMapsUrl(
        origin: AppLocation,
        destination: AppLocation,
        waypoints: List<AppLocation> = emptyList()
    ): String {
        val base = "https://www.google.com/maps/dir/?api=1"
        val originParam = "&origin=${origin.latitude},${origin.longitude}"
        val destParam = "&destination=${destination.latitude},${destination.longitude}"
        val modeParam = "&travelmode=walking"

        val waypointsParam = if (waypoints.isNotEmpty()) {
            val wpString = waypoints.joinToString("|") { "${it.latitude},${it.longitude}" }
            "&waypoints=$wpString"
        } else ""

        return "$base$originParam$destParam$waypointsParam$modeParam"
    }

    fun openInGoogleMaps(
        context: Context,
        origin: AppLocation,
        destination: AppLocation,
        waypoints: List<AppLocation> = emptyList()
    ) {
        val url = buildGoogleMapsUrl(origin, destination, waypoints)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.setPackage("com.google.android.apps.maps")

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }
}
