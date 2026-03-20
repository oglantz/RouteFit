package com.routefit.app.data.api

import com.routefit.app.data.api.dto.ComputeRoutesRequest
import com.routefit.app.data.api.dto.ComputeRoutesResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface RoutesApiService {

    @POST("directions/v2:computeRoutes")
    suspend fun computeRoutes(
        @Header("X-Goog-Api-Key") apiKey: String,
        @Header("X-Goog-FieldMask") fieldMask: String =
            "routes.distanceMeters,routes.duration,routes.polyline.encodedPolyline",
        @Body request: ComputeRoutesRequest
    ): ComputeRoutesResponse
}
