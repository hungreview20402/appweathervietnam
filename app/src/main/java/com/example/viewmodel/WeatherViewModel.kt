package com.example.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiServiceClient
import com.example.data.WeatherDatabase
import com.example.data.WeatherCity
import com.example.data.WeatherRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WeatherViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: WeatherRepository
    
    val allCitiesFlow: StateFlow<List<WeatherCity>>
    val favoriteCitiesFlow: StateFlow<List<WeatherCity>>
    
    var activeCity = mutableStateOf<WeatherCity?>(null)
        private set
        
    var searchQuery = mutableStateOf("")
        private set

    var isRefreshing = mutableStateOf(false)
        private set

    var aiLoading = mutableStateOf(false)
        private set

    var geminiAdvice = mutableStateOf("")
        private set

    // Beautiful default initial custom questions for Vietnam users
    val sampleQuestions = listOf(
        "Hôm nay nên mặc trang phục gì để thoải mái?",
        "Thời tiết này có hợp đi dã ngoại/du lịch không?",
        "Cần lưu ý bảo vệ sức khỏe như thế nào?",
        "Khu vực này có nguy cơ mưa ngập đường sầm uất không?"
    )

    init {
        val database = WeatherDatabase.getDatabase(application, viewModelScope)
        repository = WeatherRepository(database.weatherCityDao())
        
        allCitiesFlow = repository.allCities
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
            
        favoriteCitiesFlow = repository.favoriteCities
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Set default active city once loaded
        viewModelScope.launch {
            allCitiesFlow.collect { list ->
                if (activeCity.value == null && list.isNotEmpty()) {
                    // Try to pick first favorite, else first overall
                    activeCity.value = list.firstOrNull { it.isFavorite } ?: list.first()
                    refreshWeatherData(activeCity.value!!)
                }
            }
        }
    }

    fun selectCity(city: WeatherCity) {
        activeCity.value = city
        geminiAdvice.value = "" // Clear previous advice
        refreshWeatherData(city)
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun toggleFavorite(city: WeatherCity) {
        viewModelScope.launch {
            val updated = city.copy(isFavorite = !city.isFavorite)
            repository.updateCity(updated)
            if (activeCity.value?.id == city.id) {
                activeCity.value = updated
            }
        }
    }

    fun refreshWeatherData(city: WeatherCity) {
        viewModelScope.launch {
            isRefreshing.value = true
            val updated = repository.refreshCityWeather(city)
            activeCity.value = updated
            isRefreshing.value = false
            
            // Auto trigger weather advisory upon fetch
            if (geminiAdvice.value.isEmpty()) {
                generateWeatherAdvisory(updated)
            }
        }
    }

    fun generateWeatherAdvisory(city: WeatherCity) {
        viewModelScope.launch {
            aiLoading.value = true
            val condition = city.lastConditionText
            val advice = GeminiServiceClient.getAdvice(
                cityName = city.cityName,
                temp = city.lastTemp,
                condition = condition,
                question = "Hãy đưa ra lời khuyên thời tiết nhanh."
            )
            geminiAdvice.value = advice
            aiLoading.value = false
        }
    }

    fun askCustomQuestion(question: String) {
        val city = activeCity.value ?: return
        viewModelScope.launch {
            aiLoading.value = true
            geminiAdvice.value = "Thời Tiết Việt AI đang suy nghĩ..."
            val response = GeminiServiceClient.getAdvice(
                cityName = city.cityName,
                temp = city.lastTemp,
                condition = city.lastConditionText,
                question = question
            )
            geminiAdvice.value = response
            aiLoading.value = false
        }
    }

    fun addNewLocation(name: String, region: String, lat: Double, lon: Double) {
        viewModelScope.launch {
            val newCity = WeatherCity(
                cityName = name,
                region = region,
                latitude = lat,
                longitude = lon,
                isFavorite = false
            )
            repository.insertCity(newCity)
            selectCity(newCity)
        }
    }

    fun deleteLocation(city: WeatherCity) {
        viewModelScope.launch {
            repository.deleteCity(city)
            if (activeCity.value?.id == city.id) {
                activeCity.value = allCitiesFlow.value.firstOrNull { it.id != city.id }
            }
        }
    }
}
