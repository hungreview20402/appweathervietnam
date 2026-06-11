package com.example.api

import com.squareup.moshi.Json

data class WeatherResponse(
    val latitude: Double,
    val longitude: Double,
    @Json(name = "current_weather") val currentWeather: CurrentWeather?,
    val hourly: HourlyData?,
    val daily: DailyData?
)

data class CurrentWeather(
    val temperature: Double,
    val windspeed: Double,
    val winddirection: Double,
    val weathercode: Int,
    val time: String
)

data class HourlyData(
    val time: List<String>,
    @Json(name = "temperature_2m") val temperatures: List<Double>,
    @Json(name = "relative_humidity_2m") val humidities: List<Double>,
    @Json(name = "weather_code") val weatherCodes: List<Int>
)

data class DailyData(
    val time: List<String>,
    @Json(name = "weather_code") val weatherCodes: List<Int>,
    @Json(name = "temperature_2m_max") val maxTemps: List<Double>,
    @Json(name = "temperature_2m_min") val minTemps: List<Double>
)
