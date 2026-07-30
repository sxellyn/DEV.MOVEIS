package com.example.weatherapp.api

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import com.google.android.gms.maps.model.LatLng
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherService(private val context: Context) {

    private var weatherAPI: WeatherServiceAPI

    private val imageLoader = ImageLoader.Builder(context).allowHardware(false).build()

    init {
        val retrofitAPI = Retrofit.Builder()
            .baseUrl(WeatherServiceAPI.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        weatherAPI = retrofitAPI.create(WeatherServiceAPI::class.java)
    }

    suspend fun getName(lat: Double, lng: Double): String? = withContext(Dispatchers.IO) {
        search(String.format(Locale.US, "%.4f,%.4f", lat, lng))?.name
    }

    suspend fun getLocation(name: String): LatLng? = withContext(Dispatchers.IO) {
        search(name)?.let { LatLng(it.lat!!, it.lon!!) }
    }

    private fun search(query: String): APILocation? {
        val call: Call<List<APILocation>?> = weatherAPI.search(
            apiKey = WeatherServiceAPI.API_KEY,
            query = query
        )
        val response = call.execute()
        if (!response.isSuccessful) return null
        val apiLoc = response.body()
        return if (!apiLoc.isNullOrEmpty()) apiLoc[0] else null
    }

    private fun executeWeather(call: Call<APICurrentWeather?>): APICurrentWeather? {
        val response = call.execute()
        if (!response.isSuccessful) return null
        return response.body()
    }

    private fun executeForecast(call: Call<APIWeatherForecast?>): APIWeatherForecast? {
        val response = call.execute()
        if (!response.isSuccessful) return null
        return response.body()
    }

    suspend fun getWeather(name: String): APICurrentWeather? = withContext(Dispatchers.IO) {
        executeWeather(weatherAPI.weather(apiKey = WeatherServiceAPI.API_KEY, query = name))
    }

    suspend fun getForecast(name: String): APIWeatherForecast? = withContext(Dispatchers.IO) {
        executeForecast(weatherAPI.forecast(apiKey = WeatherServiceAPI.API_KEY, name = name))
    }

    suspend fun getBitmap(imgUrl: String): Bitmap? = withContext(Dispatchers.IO) {
        val request = ImageRequest.Builder(context).data(imgUrl)
            .allowHardware(false).build()
        val response = imageLoader.execute(request)
        response.drawable?.toBitmap()
    }
}
