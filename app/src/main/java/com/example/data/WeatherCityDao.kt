package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherCityDao {
    @Query("SELECT * FROM weather_cities ORDER BY isFavorite DESC, cityName ASC")
    fun getAllCities(): Flow<List<WeatherCity>>

    @Query("SELECT * FROM weather_cities WHERE isFavorite = 1 ORDER BY cityName ASC")
    fun getFavoriteCities(): Flow<List<WeatherCity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCity(city: WeatherCity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCities(cities: List<WeatherCity>)

    @Update
    suspend fun updateCity(city: WeatherCity)

    @Delete
    suspend fun deleteCity(city: WeatherCity)

    @Query("SELECT COUNT(*) FROM weather_cities")
    suspend fun getCityCount(): Int

    @Query("SELECT * FROM weather_cities WHERE cityName LIKE :searchQuery OR region LIKE :searchQuery")
    fun searchCities(searchQuery: String): Flow<List<WeatherCity>>
}
