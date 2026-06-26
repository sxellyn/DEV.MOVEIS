package com.example.weatherapp.api

import android.util.Log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherService {

    private var weatherAPI: WeatherServiceAPI

    init {
        val retrofitAPI = Retrofit.Builder()
            .baseUrl(WeatherServiceAPI.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        weatherAPI = retrofitAPI.create(WeatherServiceAPI::class.java)
    }

    private fun <T> enqueue(call: Call<T?>, onResponse: ((T?) -> Unit)? = null) {
        call.enqueue(object : Callback<T?> {
            override fun onResponse(call: Call<T?>, response: Response<T?>) {
                val obj: T? = response.body()
                onResponse?.invoke(obj)
            }

            override fun onFailure(call: Call<T?>, t: Throwable) {
                Log.w("WeatherApp WARNING", "" + t.message)
            }
        })
    }

    fun getName(lat: Double, lng: Double, onResponse: (String?) -> Unit) {
        search("$lat,$lng") { loc -> onResponse(loc?.name) }
    }

    fun getLocation(name: String, onResponse: (lat: Double?, long: Double?) -> Unit) {
        search(name) { loc -> onResponse(loc?.lat, loc?.lon) }
    }

    fun getWeather(name: String, onResponse: (APICurrentWeather?) -> Unit) {
        val call: Call<APICurrentWeather?> = weatherAPI.weather(name)
        enqueue(call) { onResponse.invoke(it) }
    }

    fun getForecast(name: String, onResponse: (APIWeatherForecast?) -> Unit) {
        val call: Call<APIWeatherForecast?> = weatherAPI.forecast(name)
        enqueue(call) { onResponse.invoke(it) }
    }

    private fun search(query: String, onResponse: (APILocation?) -> Unit) {
        val call: Call<List<APILocation>?> = weatherAPI.search(query)
        enqueue(call) { list -> onResponse(list?.let { if (it.isNotEmpty()) it[0] else null }) }
    }
}
