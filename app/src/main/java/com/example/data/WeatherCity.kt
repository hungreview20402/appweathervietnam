package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather_cities")
data class WeatherCity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cityName: String,
    val region: String,
    val latitude: Double,
    val longitude: Double,
    val isFavorite: Boolean = false,
    val lastTemp: Double = 0.0,
    val lastConditionCode: Int = 0,
    val lastConditionText: String = "Chưa có dữ liệu",
    val lastUpdated: Long = 0L
)
