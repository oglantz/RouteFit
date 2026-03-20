package com.routefit.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.routefit.app.data.api.RoutesApiService
import com.routefit.app.data.repository.RouteRepository
import com.routefit.app.domain.RouteGenerator
import com.routefit.app.model.AppLocation
import com.routefit.app.model.DistanceUnit
import com.routefit.app.model.RouteInfo
import com.routefit.app.utils.DistanceUtils
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    // Search state
    private val _startSearchQuery = MutableStateFlow("")
    val startSearchQuery: StateFlow<String> = _startSearchQuery.asStateFlow()

    private val _endSearchQuery = MutableStateFlow("")
    val endSearchQuery: StateFlow<String> = _endSearchQuery.asStateFlow()

    private val _startPredictions = MutableStateFlow<List<AutocompletePrediction>>(emptyList())
    val startPredictions: StateFlow<List<AutocompletePrediction>> = _startPredictions.asStateFlow()

    private val _endPredictions = MutableStateFlow<List<AutocompletePrediction>>(emptyList())
    val endPredictions: StateFlow<List<AutocompletePrediction>> = _endPredictions.asStateFlow()

    // Emits a LatLng whenever the camera should animate to a searched location
    private val _cameraTarget = MutableSharedFlow<LatLng>()
    val cameraTarget: SharedFlow<LatLng> = _cameraTarget.asSharedFlow()

    enum class SelectionMode { START, END, NONE }

    private var routeRepository: RouteRepository? = null
    private var placesClient: PlacesClient? = null
    private var startSearchJob: Job? = null
    private var endSearchJob: Job? = null

    fun initialize(apiKey: String, client: PlacesClient) {
        placesClient = client
        if (routeRepository != null) return

        viewModelScope.launch(Dispatchers.IO) {
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            val httpClient = OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl("https://routes.googleapis.com/")
                .client(httpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            val apiService = retrofit.create(RoutesApiService::class.java)
            routeRepository = RouteRepository(apiService, apiKey)
        }
    }

    // region Location setting

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

    fun onMapTap(latLng: LatLng) {
        val location = AppLocation(latLng.latitude, latLng.longitude)
        when (_selectionMode.value) {
            SelectionMode.START -> {
                _startSearchQuery.value = ""
                _startPredictions.value = emptyList()
                setStartLocation(location)
            }
            SelectionMode.END -> {
                _endSearchQuery.value = ""
                _endPredictions.value = emptyList()
                setEndLocation(location)
            }
            SelectionMode.NONE -> {}
        }
    }

    // endregion

    // region Search / autocomplete

    fun onStartSearchChange(query: String) {
        _startSearchQuery.value = query
        startSearchJob?.cancel()
        if (query.isBlank()) {
            _startPredictions.value = emptyList()
            return
        }
        startSearchJob = viewModelScope.launch {
            delay(300)
            fetchPredictions(query, isStart = true)
        }
    }

    fun onEndSearchChange(query: String) {
        _endSearchQuery.value = query
        endSearchJob?.cancel()
        if (query.isBlank()) {
            _endPredictions.value = emptyList()
            return
        }
        endSearchJob = viewModelScope.launch {
            delay(300)
            fetchPredictions(query, isStart = false)
        }
    }

    private fun fetchPredictions(query: String, isStart: Boolean) {
        val client = placesClient ?: return
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .build()
        client.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                if (isStart) {
                    _startPredictions.value = response.autocompletePredictions
                } else {
                    _endPredictions.value = response.autocompletePredictions
                }
            }
            .addOnFailureListener { /* Silently ignore prediction errors */ }
    }

    fun selectPrediction(placeId: String, primaryText: String, isStart: Boolean) {
        val client = placesClient ?: return
        val placeFields = listOf(Place.Field.LOCATION, Place.Field.ADDRESS)
        val request = FetchPlaceRequest.newInstance(placeId, placeFields)
        client.fetchPlace(request)
            .addOnSuccessListener { response ->
                val place = response.place
                val latLng = place.location ?: return@addOnSuccessListener
                val address = place.formattedAddress ?: primaryText
                val location = AppLocation(latLng.latitude, latLng.longitude, address)
                if (isStart) {
                    _startSearchQuery.value = address
                    _startPredictions.value = emptyList()
                    setStartLocation(location)
                } else {
                    _endSearchQuery.value = address
                    _endPredictions.value = emptyList()
                    setEndLocation(location)
                }
                viewModelScope.launch { _cameraTarget.emit(latLng) }
            }
            .addOnFailureListener { e ->
                _error.value = "Could not find location: ${e.message}"
            }
    }

    fun clearStartSearch() {
        startSearchJob?.cancel()
        _startSearchQuery.value = ""
        _startPredictions.value = emptyList()
        _startLocation.value = null
        _selectionMode.value = SelectionMode.START
        clearRoutes()
    }

    fun clearEndSearch() {
        endSearchJob?.cancel()
        _endSearchQuery.value = ""
        _endPredictions.value = emptyList()
        _endLocation.value = null
        if (_startLocation.value != null) _selectionMode.value = SelectionMode.END
        clearRoutes()
    }

    // endregion

    fun setTargetDistance(value: String) {
        _targetDistance.value = value
    }

    fun setDistanceUnit(unit: DistanceUnit) {
        _distanceUnit.value = unit
    }

    fun selectRoute(index: Int) {
        _selectedRouteIndex.value = index
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
        _startSearchQuery.value = ""
        _endSearchQuery.value = ""
        _startPredictions.value = emptyList()
        _endPredictions.value = emptyList()
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
