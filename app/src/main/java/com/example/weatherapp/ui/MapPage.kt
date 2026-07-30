package com.example.weatherapp.ui

import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.weatherapp.R
import com.example.weatherapp.model.Weather
import com.example.weatherapp.viewmodel.MainViewModel
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState

@Composable
fun MapPage(modifier: Modifier = Modifier, viewModel: MainViewModel) {
    val context = LocalContext.current

    val hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val camPosState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(-8.05, -34.9), 7f)
    }

    val cities by viewModel.cities.collectAsStateWithLifecycle()
    val weatherMap by viewModel.weather.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = camPosState,
            properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
            uiSettings = MapUiSettings(myLocationButtonEnabled = true),
            onMapClick = { latLng ->
                Toast.makeText(context, "Buscando cidade...", Toast.LENGTH_SHORT).show()
                viewModel.addCity(latLng) { name, saved, error ->
                    val msg = when {
                        name == null -> "Cidade não encontrada"
                        !saved -> "Erro ao salvar: ${error ?: "Firebase"}"
                        else -> "$name adicionada!"
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            }
        ) {
            cities.values.forEach { city ->
                val location = city.location ?: return@forEach
                key(city.name, cities.size) {
                    val weather = weatherMap[city.name] ?: Weather.LOADING
                    LaunchedEffect(city.name) {
                        viewModel.loadWeather(city.name)
                    }
                    LaunchedEffect(weather) {
                        viewModel.loadBitmap(city.name)
                    }
                    val image = weather.bitmap
                        ?: ContextCompat.getDrawable(context, R.drawable.loading)!!.toBitmap()
                    val marker = BitmapDescriptorFactory.fromBitmap(image.scale(120, 120))
                    val desc = if (weather == Weather.LOADING) "Carregando clima..." else weather.desc
                    Marker(
                        state = rememberUpdatedMarkerState(position = location),
                        icon = marker,
                        title = city.name,
                        snippet = desc
                    )
                }
            }
        }
    }
}
