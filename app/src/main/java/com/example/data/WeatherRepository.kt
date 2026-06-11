package com.example.data

import com.example.api.WeatherApiClient
import kotlinx.coroutines.flow.Flow

class WeatherRepository(private val weatherCityDao: WeatherCityDao) {
    val allCities: Flow<List<WeatherCity>> = weatherCityDao.getAllCities()
    val favoriteCities: Flow<List<WeatherCity>> = weatherCityDao.getFavoriteCities()

    fun searchCities(query: String): Flow<List<WeatherCity>> {
        return weatherCityDao.searchCities("%$query%")
    }

    suspend fun insertCity(city: WeatherCity) {
        weatherCityDao.insertCity(city)
    }

    suspend fun updateCity(city: WeatherCity) {
        weatherCityDao.updateCity(city)
    }

    suspend fun deleteCity(city: WeatherCity) {
        weatherCityDao.deleteCity(city)
    }

    suspend fun refreshCityWeather(city: WeatherCity): WeatherCity {
        try {
            val response = WeatherApiClient.service.getWeatherForecast(city.latitude, city.longitude)
            val current = response.currentWeather
            if (current != null) {
                val updatedCity = city.copy(
                    lastTemp = current.temperature,
                    lastConditionCode = current.weathercode,
                    lastConditionText = translateWeatherCode(current.weathercode),
                    lastUpdated = System.currentTimeMillis()
                )
                weatherCityDao.updateCity(updatedCity)
                return updatedCity
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return city
    }

    fun translateWeatherCode(code: Int): String {
        return when (code) {
            0 -> "Trời quang mây tạnh"
            1, 2 -> "Trời ít mây"
            3 -> "Nhiều mây nhiều nơi"
            45, 48 -> "Sương mù hạn chế tầm nhìn"
            51, 53, 55 -> "Mưa phùn sương mờ"
            61 -> "Mưa nhỏ rải rác"
            63 -> "Mưa vừa tầm tã"
            65 -> "Mưa to xối xả"
            71, 73, 75 -> "Mưa tuyết lạnh (Sa Pa)"
            80, 81, 82 -> "Mưa rào rải rác"
            95, 96, 99 -> "Giông bão kèm sấm sét"
            else -> "Thời tiết mây dông"
        }
    }
}
