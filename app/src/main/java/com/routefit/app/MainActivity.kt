package com.routefit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import com.google.android.libraries.places.api.Places
import com.routefit.app.ui.screens.LandingScreen
import com.routefit.app.ui.screens.MapScreen
import com.routefit.app.ui.theme.RouteFitTheme
import com.routefit.app.viewmodel.MapViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MapViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            RouteFitTheme {
                var showLanding by rememberSaveable { mutableStateOf(true) }

                if (showLanding) {
                    LandingScreen(
                        onContinue = { showLanding = false }
                    )
                } else {
                    MapScreen(viewModel = viewModel)
                }
            }
        }

        // Defer SDK init until after first frame to avoid launch black screens
        // on slower emulators/devices.
        window.decorView.post {
            if (!Places.isInitialized()) {
                Places.initializeWithNewPlacesApiEnabled(this, BuildConfig.MAPS_API_KEY)
            }
            val placesClient = Places.createClient(this)
            viewModel.initialize(BuildConfig.MAPS_API_KEY, placesClient)
        }
    }
}
