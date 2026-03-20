package com.routefit.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
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
import kotlinx.coroutines.launch

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
            initialValue = SheetValue.Expanded
        )
    )
    val sheetScope = rememberCoroutineScope()

    val routeColors = listOf(
        Color(0xFF1976D2),
        Color(0xFF4CAF50),
        Color(0xFFFF9800)
    )

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 280.dp,
        sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        sheetShadowElevation = 12.dp,
        sheetContent = {
            val isCollapsed = scaffoldState.bottomSheetState.currentValue != SheetValue.Expanded
            val startDisplayText = startLocation?.address ?: startSearchQuery
            val endDisplayText = endLocation?.address ?: endSearchQuery
            val selectedDistanceText = generatedRoutes.getOrNull(selectedRouteIndex)?.let { route ->
                DistanceUtils.formatDistance(route.distanceMeters.toDouble(), distanceUnit)
            } ?: "-- ${distanceUnit.abbreviation}"

            if (isCollapsed) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                ) {
                    val compact = maxWidth < 360.dp
                    val fieldHeight = if (compact) 42.dp else 48.dp
                    val fieldSpacing = if (compact) 4.dp else 6.dp
                    val sectionPadding = if (compact) 10.dp else 12.dp

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            tonalElevation = 3.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = sectionPadding, vertical = sectionPadding),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Directions",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    if (startLocation != null || endLocation != null) {
                                        TextButton(onClick = { viewModel.resetMarkers() }) {
                                            Text("Reset")
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(fieldSpacing))

                                CollapsedLocationField(
                                    text = startDisplayText.ifBlank { "Start location" },
                                    placeholder = "Start location",
                                    tint = Color(0xFF4CAF50),
                                    height = fieldHeight,
                                    onClear = { viewModel.clearStartSearch() },
                                    onTap = {
                                        sheetScope.launch {
                                            scaffoldState.bottomSheetState.expand()
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.height(fieldSpacing))

                                CollapsedLocationField(
                                    text = endDisplayText.ifBlank { "End location" },
                                    placeholder = "End location",
                                    tint = Color(0xFFF44336),
                                    height = fieldHeight,
                                    onClear = { viewModel.clearEndSearch() },
                                    onTap = {
                                        sheetScope.launch {
                                            scaffoldState.bottomSheetState.expand()
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.height(fieldSpacing))

                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = "Distance: $selectedDistanceText",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Directions",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        if (startLocation != null || endLocation != null) {
                            TextButton(onClick = { viewModel.resetMarkers() }) {
                                Text("Reset")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

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

                    Box(
                        modifier = Modifier
                            .padding(start = 18.dp)
                            .width(2.dp)
                            .height(12.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

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

                    Spacer(modifier = Modifier.height(12.dp))

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Spacer(modifier = Modifier.height(12.dp))

                    DistanceInput(
                        distance = targetDistance,
                        onDistanceChange = { viewModel.setTargetDistance(it) },
                        unit = distanceUnit,
                        onUnitChange = { viewModel.setDistanceUnit(it) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.generateRoutes() },
                        enabled = viewModel.canGenerateRoutes() && !isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
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
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Route Options",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))

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

                    Spacer(modifier = Modifier.height(10.dp))
                }
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

@Composable
private fun CollapsedLocationField(
    text: String,
    placeholder: String,
    tint: Color,
    height: androidx.compose.ui.unit.Dp,
    onClear: () -> Unit,
    onTap: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onTap)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text.ifBlank { placeholder },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (text.isNotBlank()) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(onClick = onClear)
                )
            }
        }
    }
}
