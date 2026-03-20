package com.routefit.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.routefit.app.data.api.RoutesApiService
import com.routefit.app.data.repository.RouteRepository
import com.routefit.app.domain.RouteGenerator
import com.routefit.app.model.AppLocation
import com.routefit.app.model.DistanceUnit
import com.routefit.app.model.RouteInfo
import com.routefit.app.utils.DistanceUtils
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class MapViewModel : ViewModel() {

    private val _startLocation = MutableStateFlow<AppLocation?>(null)
    val startLocation: StateFlow<AppLocation?> = _startLocation.asStateFlow()

    private val _endLocation = MutableStateFlow<AppLocation?>(null)
    val endLocation: StateFlow<AppLocation?> = _endLocation.asStateFlow()

    private val _targetDistance = MutableStateFlow("")
    val targetDistance: StateFlow<String> = _targetDistance.asStateFlow()

    private val _distanceUnit = MutableStateFlow(DistanceUnit.MILES)
    val distanceUnit: StateFlow<DistanceUnit> = _distanceUnit.asStateFlow()

    private val _generatedRoutes = MutableStateFlow<List<RouteInfo>>(emptyList())
    val generatedRoutes: StateFlow<List<RouteInfo>> = _generatedRoutes.asStateFlow()

    private val _selectedRouteIndex = MutableStateFlow(-1)
    val selectedRouteIndex: StateFlow<Int> = _selectedRouteIndex.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _selectionMode = MutableStateFlow(SelectionMode.START)
    val selectionMode: StateFlow<SelectionMode> = _selectionMode.asStateFlow()

    enum class SelectionMode { START, END, NONE }

    private var routeRepository: RouteRepository? = null

    fun initialize(apiKey: String) {
        if (routeRepository != null) return

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val client = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://routes.googleapis.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        val apiService = retrofit.create(RoutesApiService::class.java)
        routeRepository = RouteRepository(apiService, apiKey)
    }

    fun setStartLocation(location: AppLocation) {
        _startLocation.value = location
        _selectionMode.value =
            if (_endLocation.value == null) SelectionMode.END else SelectionMode.NONE
        clearRoutes()
    }

    fun setEndLocation(location: AppLocation) {
        _endLocation.value = location
        _selectionMode.value = SelectionMode.NONE
        clearRoutes()
    }

    fun setTargetDistance(value: String) {
        _targetDistance.value = value
    }

    fun setDistanceUnit(unit: DistanceUnit) {
        _distanceUnit.value = unit
    }

    fun selectRoute(index: Int) {
        _selectedRouteIndex.value = index
    }

    fun onMapTap(latLng: LatLng) {
        val location = AppLocation(latLng.latitude, latLng.longitude)
        when (_selectionMode.value) {
            SelectionMode.START -> setStartLocation(location)
            SelectionMode.END -> setEndLocation(location)
            SelectionMode.NONE -> {}
        }
    }

    fun generateRoutes() {
        val start = _startLocation.value ?: return
        val end = _endLocation.value ?: return
        val distance = _targetDistance.value.toDoubleOrNull() ?: return
        if (distance <= 0) return

        val repo = routeRepository ?: return
        val targetMeters = DistanceUtils.unitToMeters(distance, _distanceUnit.value)
        val toleranceMeters = DistanceUtils.milesToMeters(0.25)

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _generatedRoutes.value = emptyList()
            _selectedRouteIndex.value = -1

            try {
                val generator = RouteGenerator(object : RouteGenerator.RouteProvider {
                    override suspend fun getWalkingRoute(
                        origin: AppLocation,
                        destination: AppLocation,
                        waypoints: List<AppLocation>
                    ): RouteInfo? = repo.computeWalkingRoute(origin, destination, waypoints)
                })

                val routes = generator.generateRoutes(start, end, targetMeters, toleranceMeters)
                _generatedRoutes.value = routes
                if (routes.isNotEmpty()) {
                    _selectedRouteIndex.value = 0
                }
                if (routes.isEmpty()) {
                    _error.value = "No routes found matching your criteria. Try adjusting the distance."
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to generate routes"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetMarkers() {
        _startLocation.value = null
        _endLocation.value = null
        _selectionMode.value = SelectionMode.START
        clearRoutes()
    }

    private fun clearRoutes() {
        _generatedRoutes.value = emptyList()
        _selectedRouteIndex.value = -1
        _error.value = null
    }

    fun canGenerateRoutes(): Boolean {
        return _startLocation.value != null &&
                _endLocation.value != null &&
                (_targetDistance.value.toDoubleOrNull() ?: 0.0) > 0
    }
}
