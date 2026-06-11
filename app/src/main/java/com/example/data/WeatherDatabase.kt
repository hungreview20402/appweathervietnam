package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [WeatherCity::class], version = 1, exportSchema = false)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun weatherCityDao(): WeatherCityDao

    companion object {
        @Volatile
        private var INSTANCE: WeatherDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): WeatherDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WeatherDatabase::class.java,
                    "vietnam_weather_database"
                )
                .addCallback(WeatherDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class WeatherDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.weatherCityDao())
                }
            }
        }

        suspend fun populateDatabase(weatherCityDao: WeatherCityDao) {
            val initialCities = listOf(
                WeatherCity(cityName = "Hà Nội", region = "Miền Bắc", latitude = 21.0285, longitude = 105.8542, isFavorite = true),
                WeatherCity(cityName = "TP. Hồ Chí Minh", region = "Miền Nam", latitude = 10.8231, longitude = 106.6297, isFavorite = true),
                WeatherCity(cityName = "Đà Nẵng", region = "Miền Trung", latitude = 16.0544, longitude = 108.2022, isFavorite = true),
                WeatherCity(cityName = "Hải Phòng", region = "Miền Bắc", latitude = 20.8449, longitude = 106.6881, isFavorite = false),
                WeatherCity(cityName = "Cần Thơ", region = "Miền Nam", latitude = 10.0452, longitude = 105.7469, isFavorite = false),
                WeatherCity(cityName = "Nha Trang", region = "Duyên hải Nam Trung Bộ", latitude = 12.2388, longitude = 109.1967, isFavorite = false),
                WeatherCity(cityName = "Đà Lạt", region = "Tây Nguyên", latitude = 11.9404, longitude = 108.4583, isFavorite = true),
                WeatherCity(cityName = "Sa Pa", region = "Tây Bắc Bộ", latitude = 22.3364, longitude = 103.8438, isFavorite = true),
                WeatherCity(cityName = "Huế", region = "Miền Trung", latitude = 16.4637, longitude = 107.5909, isFavorite = false),
                WeatherCity(cityName = "Phú Quốc", region = "Miền Nam (Đảo)", latitude = 10.2899, longitude = 104.0049, isFavorite = false)
            )
            weatherCityDao.insertCities(initialCities)
        }
    }
}
