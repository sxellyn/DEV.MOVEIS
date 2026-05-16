package com.example.weatherapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weatherapp.model.City
import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import com.example.weatherapp.viewmodel.MainViewModel

private fun getCities() = List(20) { i ->
    City(
        name = "Cidade $i",
        weather = "Carregando clima..."
    )
}

@Composable
fun CityItem(
    city: City,
    onClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            Icons.Rounded.FavoriteBorder,
            contentDescription = ""
        )

        Spacer(modifier = Modifier.size(12.dp))

        Column(
            modifier = modifier.weight(1f)
        ) {

            Text(
                modifier = Modifier,
                text = city.name,
                fontSize = 24.sp
            )

            Text(
                modifier = Modifier,
                text = city.weather ?: "Carregando clima...",
                fontSize = 16.sp
            )
        }

        IconButton(
            onClick = onClose
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Close"
            )
        }
    }
}

@Composable
fun ListPage(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel
) {
    val cityList = viewModel.cities
    val activity = LocalActivity.current as Activity

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        items(cityList, key = { it.name }) { city ->
            CityItem(
                city = city,
                onClose = {
                    Toast.makeText(
                        activity,
                        "${city.name} Removida",
                        Toast.LENGTH_SHORT
                    ).show()

                    viewModel.remove(city)
                },
                onClick = {
                    Toast.makeText(
                        activity,
                        "Cidade selecionada: ${city.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }
}