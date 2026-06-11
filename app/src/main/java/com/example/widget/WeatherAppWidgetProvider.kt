package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.api.WeatherApiClient
import com.example.data.WeatherDatabase
import com.example.data.WeatherCity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeatherAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            updateAllWidgets(context, appWidgetManager, appWidgetIds)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, WeatherAppWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                
                // Fetch new weather and update
                refreshWeatherData(context)
                updateAllWidgets(context, appWidgetManager, appWidgetIds)
            }
        }
    }

    private suspend fun refreshWeatherData(context: Context) {
        try {
            val db = WeatherDatabase.getDatabase(context, CoroutineScope(Dispatchers.IO))
            val dao = db.weatherCityDao()
            val favorites = dao.getFavoriteCities().firstOrNull() ?: emptyList()
            val activeCity = favorites.firstOrNull() ?: dao.getAllCities().firstOrNull()?.firstOrNull()
            
            if (activeCity != null) {
                val response = WeatherApiClient.service.getWeatherForecast(activeCity.latitude, activeCity.longitude)
                val current = response.currentWeather
                if (current != null) {
                    val updated = activeCity.copy(
                        lastTemp = current.temperature,
                        lastConditionCode = current.weathercode,
                        lastConditionText = translateWeatherCode(current.weathercode),
                        lastUpdated = System.currentTimeMillis()
                    )
                    dao.updateCity(updated)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun updateAllWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val db = WeatherDatabase.getDatabase(context, CoroutineScope(Dispatchers.IO))
        val dao = db.weatherCityDao()
        val favorites = dao.getFavoriteCities().firstOrNull() ?: emptyList()
        val activeCity = favorites.firstOrNull() ?: dao.getAllCities().firstOrNull()?.firstOrNull()

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.weather_widget)
            
            if (activeCity != null) {
                val tempText = if (activeCity.lastUpdated > 0L) "${activeCity.lastTemp.toInt()}°C" else "--°C"
                views.setTextViewText(R.id.widget_location, activeCity.cityName)
                views.setTextViewText(R.id.widget_temp, tempText)
                views.setTextViewText(R.id.widget_condition, activeCity.lastConditionText)
                
                val timeStr = if (activeCity.lastUpdated > 0L) {
                    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                    "Cập nhật: " + sdf.format(Date(activeCity.lastUpdated))
                } else {
                    "Chưa cập nhật"
                }
                views.setTextViewText(R.id.widget_time, timeStr)
            } else {
                views.setTextViewText(R.id.widget_location, "Thời Tiết Việt")
                views.setTextViewText(R.id.widget_temp, "--°C")
                views.setTextViewText(R.id.widget_condition, "Vui lòng mở ứng dụng")
                views.setTextViewText(R.id.widget_time, "")
            }

            // Click action to open MainActivity
            val appIntent = Intent(context, MainActivity::class.java)
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getActivity(context, 0, appIntent, flags)
            views.setOnClickPendingIntent(R.id.widget_location, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_temp, pendingIntent)

            // Click action for Refresh button
            val refreshIntent = Intent(context, WeatherAppWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context, 1, refreshIntent, flags
            )
            views.setOnClickPendingIntent(R.id.widget_refresh, refreshPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun translateWeatherCode(code: Int): String {
        return when (code) {
            0 -> "Trại quang mây tạnh"
            1, 2 -> "Trời ít mây"
            3 -> "Nhiều mây"
            45, 48 -> "Sương mù tầm nhìn kém"
            51, 53, 55 -> "Mưa phùn nhẹ"
            61 -> "Mưa nhỏ rải rác"
            63 -> "Mưa vừa phải"
            65 -> "Mưa to xối xả"
            71, 73, 75 -> "Tuyết lạnh phủ Sa Pa"
            80, 81, 82 -> "Mưa rào sấm chớp"
            95, 96, 99 -> "Giông bão sấm sét"
            else -> "Nhiều mây dông"
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.example.action.REFRESH_WIDGET"
    }
}
