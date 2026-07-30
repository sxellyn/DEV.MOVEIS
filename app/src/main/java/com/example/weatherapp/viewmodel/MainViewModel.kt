package com.example.weatherapp.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.weatherapp.api.WeatherService
import com.example.weatherapp.api.toForecast
import com.example.weatherapp.api.toWeather
import com.example.weatherapp.model.City
import com.example.weatherapp.model.Forecast
import com.example.weatherapp.model.User
import com.example.weatherapp.model.Weather
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.example.weatherapp.monitor.ForecastMonitor
import com.example.weatherapp.repo.Repository
import com.example.weatherapp.ui.nav.Route
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(
    private val repo: Repository,
    private val service: WeatherService,
    private val monitor: ForecastMonitor
) : ViewModel() {

    private var _city = mutableStateOf<String?>(null)
    var city: String?
        get() = _city.value
        set(tmp) { _city.value = tmp }

    private var _page = mutableStateOf<Route>(Route.Home)
    var page: Route
        get() = _page.value
        set(tmp) { _page.value = tmp }

    private val _cities: kotlinx.coroutines.flow.Flow<Map<String, City>> = repo.cities.map { cityList ->
        cityList.associateBy { it.name }
    }
    val cities = _cities.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _weather = MutableStateFlow<Map<String, Weather>>(emptyMap())
    val weather = _weather.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _forecast = MutableStateFlow<Map<String, List<Forecast>?>>(emptyMap())
    val forecast = _forecast.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val user = repo.user.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), authUser())

    private fun authUser(): User? {
        val current = Firebase.auth.currentUser ?: return null
        val name = current.displayName?.takeIf { it.isNotBlank() }
            ?: current.email?.substringBefore('@')
            ?: "Usuário"
        return User(name = name, email = current.email ?: "")
    }

    fun remove(city: City) {
        repo.remove(city)
        monitor.cancelCity(city)
    }

    fun update(city: City) {
        repo.update(city)
        monitor.updateCity(city)
    }

    fun addCity(name: String, onResult: (String?, Boolean, String?) -> Unit = { _, _, _ -> }) =
        viewModelScope.launch(Dispatchers.IO) {
            val location = service.getLocation(name)
            if (location == null) {
                withContext(Dispatchers.Main) { onResult(null, false, null) }
                return@launch
            }
            val city = City(name = name, location = location)
            val result = repo.add(city)
            withContext(Dispatchers.Main) {
                onResult(name, result.isSuccess, result.exceptionOrNull()?.message)
            }
        }

    fun addCity(location: LatLng, onResult: (String?, Boolean, String?) -> Unit = { _, _, _ -> }) =
        viewModelScope.launch(Dispatchers.IO) {
            val name = service.getName(location.latitude, location.longitude)
            if (name == null) {
                withContext(Dispatchers.Main) { onResult(null, false, null) }
                return@launch
            }
            val city = City(name = name, location = location)
            val result = repo.add(city)
            withContext(Dispatchers.Main) {
                onResult(name, result.isSuccess, result.exceptionOrNull()?.message)
            }
        }

    fun loadWeather(name: String) {
        if (_weather.value[name] != null) return
        viewModelScope.launch(Dispatchers.Main) {
            _weather.update { current -> current + (name to Weather.LOADING) }
            runCatching {
                service.getWeather(name)?.toWeather()
            }.onSuccess { weather ->
                _weather.update { curr -> curr + (name to (weather ?: Weather.ERROR)) }
            }.onFailure {
                _weather.update { curr -> curr + (name to Weather.ERROR) }
            }
        }
    }

    fun loadForecast(name: String) {
        if (_forecast.value[name] != null) return
        viewModelScope.launch(Dispatchers.Main) {
            _forecast.update { current -> current + (name to emptyList()) }
            runCatching {
                service.getForecast(name)?.toForecast()
            }.onSuccess { forecast ->
                _forecast.update { curr -> curr + (name to forecast) }
            }.onFailure {
                _forecast.update { curr -> curr + (name to null) }
            }
        }
    }

    fun loadBitmap(name: String) {
        val weather = _weather.value[name]
        if (weather == null || weather == Weather.LOADING || weather == Weather.ERROR ||
            weather.bitmap != null
        ) return
        viewModelScope.launch(Dispatchers.Main) {
            runCatching {
                service.getBitmap(weather.imgUrl)
            }.onSuccess { bitmap ->
                _weather.update { curr -> curr + (name to weather.copy(bitmap = bitmap)) }
            }
        }
    }
}

class MainViewModelFactory(
    private val repo: Repository,
    private val service: WeatherService,
    private val monitor: ForecastMonitor
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repo, service, monitor) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
