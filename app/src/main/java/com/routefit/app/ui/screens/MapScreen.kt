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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.routefit.app.ui.components.DistanceInput
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

    val context = LocalContext.current
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(37.7749, -122.4194), 14f)
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
        sheetPeekHeight = 220.dp,
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
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

                Spacer(modifier = Modifier.height(12.dp))

                val hintText = when {
                    startLocation == null -> "Tap the map to set your starting point"
                    endLocation == null -> "Tap the map to set your ending point"
                    else -> "Set your target distance and generate routes"
                }
                Text(
                    text = hintText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                startLocation?.let { loc ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Start: %.4f, %.4f".format(loc.latitude, loc.longitude),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                endLocation?.let { loc ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            tint = Color(0xFFF44336),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "End: %.4f, %.4f".format(loc.latitude, loc.longitude),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

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
                        title = "Start",
                        icon = BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_GREEN
                        )
                    )
                }

                endLocation?.let {
                    Marker(
                        state = MarkerState(position = LatLng(it.latitude, it.longitude)),
                        title = "End",
                        icon = BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_RED
                        )
                    )
                }

                generatedRoutes.forEachIndexed { index, route ->
                    if (route.polylinePoints.isNotEmpty()) {
                        Polyline(
                            points = route.polylinePoints,
                            color = if (index == selectedRouteIndex)
                                routeColors.getOrElse(index) { Color.Blue }
                            else
                                routeColors.getOrElse(index) { Color.Gray }.copy(alpha = 0.4f),
                            width = if (index == selectedRouteIndex) 14f else 8f
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (startLocation != null || endLocation != null) {
                    SmallFloatingActionButton(
                        onClick = { viewModel.resetMarkers() },
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Reset markers",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

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
