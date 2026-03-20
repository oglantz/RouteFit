package com.routefit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.google.android.libraries.places.api.Places
import com.routefit.app.ui.screens.MapScreen
import com.routefit.app.ui.theme.RouteFitTheme
import com.routefit.app.viewmodel.MapViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MapViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(this, BuildConfig.MAPS_API_KEY)
        }
        val placesClient = Places.createClient(this)

        setContent {
            RouteFitTheme {
                MapScreen(viewModel = viewModel)
            }
        }

        viewModel.initialize(BuildConfig.MAPS_API_KEY, placesClient)
    }
}
