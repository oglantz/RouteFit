package com.routefit.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.routefit.app.ui.components.DistanceInput
import com.routefit.app.ui.components.LocationSearchBar
import com.routefit.app.ui.components.RouteCard
import com.routefit.app.utils.DistanceUtils
import com.routefit.app.utils.GoogleMapsExporter
import com.routefit.app.viewmodel.MapViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(viewModel: MapViewModel) {
    val startLocation by viewModel.startLocation.collectAsStateWithLifecycle()
    val endLocation by viewModel.endLocation.collectAsStateWithLifecycle()
    val targetDistance by viewModel.targetDistance.collectAsStateWithLifecycle()
    val distanceUnit by viewModel.distanceUnit.collectAsStateWithLifecycle()
    val generatedRoutes by viewModel.generatedRoutes.collectAsStateWithLifecycle()
    val selectedRouteIndex by viewModel.selectedRouteIndex.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val selectionMode by viewModel.selectionMode.collectAsStateWithLifecycle()
    val startSearchQuery by viewModel.startSearchQuery.collectAsStateWithLifecycle()
    val endSearchQuery by viewModel.endSearchQuery.collectAsStateWithLifecycle()
    val startPredictions by viewModel.startPredictions.collectAsStateWithLifecycle()
    val endPredictions by viewModel.endPredictions.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(37.7749, -122.4194), 14f)
    }

    LaunchedEffect(Unit) {
        viewModel.cameraTarget.collect { target ->
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(target, 15f),
                durationMs = 600
            )
        }
    }

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded
        )
    )

    val routeColors = listOf(
        Color(0xFF1976D2),
        Color(0xFF4CAF50),
        Color(0xFFFF9800)
    )

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 280.dp,
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Title row with reset button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Directions",
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (startLocation != null || endLocation != null) {
                        TextButton(onClick = { viewModel.resetMarkers() }) {
                            Text("Reset")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Start location search
                LocationSearchBar(
                    query = startSearchQuery,
                    onQueryChange = { viewModel.onStartSearchChange(it) },
                    predictions = startPredictions,
                    onPredictionSelected = { placeId, primaryText ->
                        viewModel.selectPrediction(placeId, primaryText, isStart = true)
                    },
                    onClear = { viewModel.clearStartSearch() },
                    placeholder = "Start location",
                    leadingIconTint = Color(0xFF4CAF50),
                    modifier = Modifier.fillMaxWidth()
                )

                // Visual connector between start and end
                Box(
                    modifier = Modifier
                        .padding(start = 18.dp)
                        .width(2.dp)
                        .height(12.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                // End location search
                LocationSearchBar(
                    query = endSearchQuery,
                    onQueryChange = { viewModel.onEndSearchChange(it) },
                    predictions = endPredictions,
                    onPredictionSelected = { placeId, primaryText ->
                        viewModel.selectPrediction(placeId, primaryText, isStart = false)
                    },
                    onClear = { viewModel.clearEndSearch() },
                    placeholder = "End location",
                    leadingIconTint = Color(0xFFF44336),
                    modifier = Modifier.fillMaxWidth()
                )

                // Tap-to-set hint
                if (selectionMode != MapViewModel.SelectionMode.NONE) {
                    val hintText = when (selectionMode) {
                        MapViewModel.SelectionMode.START -> "Or tap the map to set your start"
                        MapViewModel.SelectionMode.END -> "Or tap the map to set your end"
                        else -> ""
                    }
                    Text(
                        text = hintText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Spacer(modifier = Modifier.height(16.dp))

                DistanceInput(
                    distance = targetDistance,
                    onDistanceChange = { viewModel.setTargetDistance(it) },
                    unit = distanceUnit,
                    onUnitChange = { viewModel.setDistanceUnit(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.generateRoutes() },
                    enabled = viewModel.canGenerateRoutes() && !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Generating...")
                    } else {
                        Text("Generate Routes")
                    }
                }

                error?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (generatedRoutes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Route Options",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    generatedRoutes.forEachIndexed { index, route ->
                        val targetMeters = DistanceUtils.unitToMeters(
                            targetDistance.toDoubleOrNull() ?: 0.0,
                            distanceUnit
                        )
                        RouteCard(
                            route = route,
                            index = index,
                            isSelected = index == selectedRouteIndex,
                            targetDistanceMeters = targetMeters,
                            distanceUnit = distanceUnit,
                            routeColor = routeColors.getOrElse(index) { Color.Gray },
                            onSelect = { viewModel.selectRoute(index) },
                            onExport = {
                                startLocation?.let { start ->
                                    endLocation?.let { end ->
                                        GoogleMapsExporter.openInGoogleMaps(
                                            context, start, end, route.waypoints
                                        )
                                    }
                                }
                            }
                        )
                        if (index < generatedRoutes.size - 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapClick = { latLng -> viewModel.onMapTap(latLng) }
            ) {
                startLocation?.let {
                    Marker(
                        state = MarkerState(position = LatLng(it.latitude, it.longitude)),
                        title = it.address ?: "Start",
                        icon = BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_GREEN
                        )
                    )
                }

                endLocation?.let {
                    Marker(
                        state = MarkerState(position = LatLng(it.latitude, it.longitude)),
                        title = it.address ?: "End",
                        icon = BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_RED
                        )
                    )
                }

                if (selectedRouteIndex in generatedRoutes.indices) {
                    val route = generatedRoutes[selectedRouteIndex]
                    if (route.polylinePoints.isNotEmpty()) {
                        Polyline(
                            points = route.polylinePoints,
                            color = routeColors.getOrElse(selectedRouteIndex) { Color.Blue },
                            width = 14f
                        )
                    }
                }
            }

            // Selection mode badge on the map
            if (selectionMode != MapViewModel.SelectionMode.NONE) {
                val badgeText = when (selectionMode) {
                    MapViewModel.SelectionMode.START -> "Tap to set START"
                    MapViewModel.SelectionMode.END -> "Tap to set END"
                    else -> ""
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = badgeText,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}
