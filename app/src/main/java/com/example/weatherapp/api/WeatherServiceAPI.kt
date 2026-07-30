package com.example.weatherapp.api

import com.example.weatherapp.BuildConfig
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherServiceAPI {

    companion object {
        const val BASE_URL = "https://api.weatherapi.com/v1/"
        const val API_KEY = BuildConfig.WEATHER_API_KEY
    }

    @GET("search.json")
    fun search(
        @Query("key") apiKey: String,
        @Query("q") query: String,
        @Query("lang") lang: String = "pt_br"
    ): Call<List<APILocation>?>

    @GET("current.json")
    fun weather(
        @Query("key") apiKey: String,
        @Query("q") query: String,
        @Query("lang") lang: String = "pt"
    ): Call<APICurrentWeather?>

    @GET("forecast.json")
    fun forecast(
        @Query("key") apiKey: String,
        @Query("q") name: String,
        @Query("days") days: Int = 10,
        @Query("lang") lang: String = "pt"
    ): Call<APIWeatherForecast?>
}
