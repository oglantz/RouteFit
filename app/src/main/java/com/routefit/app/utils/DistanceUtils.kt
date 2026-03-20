package com.routefit.app.utils

import com.routefit.app.model.DistanceUnit

object DistanceUtils {

    private const val METERS_PER_MILE = 1609.344
    private const val METERS_PER_KM = 1000.0

    fun metersToMiles(meters: Double): Double = meters / METERS_PER_MILE
    fun metersToKm(meters: Double): Double = meters / METERS_PER_KM
    fun milesToMeters(miles: Double): Double = miles * METERS_PER_MILE
    fun kmToMeters(km: Double): Double = km * METERS_PER_KM

    fun metersToUnit(meters: Double, unit: DistanceUnit): Double = when (unit) {
        DistanceUnit.MILES -> metersToMiles(meters)
        DistanceUnit.KILOMETERS -> metersToKm(meters)
    }

    fun unitToMeters(value: Double, unit: DistanceUnit): Double = when (unit) {
        DistanceUnit.MILES -> milesToMeters(value)
        DistanceUnit.KILOMETERS -> kmToMeters(value)
    }

    fun formatDistance(meters: Double, unit: DistanceUnit): String {
        val value = metersToUnit(meters, unit)
        return String.format("%.2f %s", value, unit.abbreviation)
    }

    fun parseDuration(duration: String): Int {
        return duration.removeSuffix("s").toIntOrNull() ?: 0
    }

    fun formatDuration(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
}
