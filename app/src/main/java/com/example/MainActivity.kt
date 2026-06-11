package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.WeatherCity
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.WeatherViewModel
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    WeatherDashboardScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherDashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: WeatherViewModel = viewModel()
) {
    val allCities by viewModel.allCitiesFlow.collectAsState()
    val activeCity = viewModel.activeCity.value
    val searchQuery = viewModel.searchQuery.value
    val isRefreshing = viewModel.isRefreshing.value
    val aiLoading = viewModel.aiLoading.value
    val geminiAdvice = viewModel.geminiAdvice.value
    
    // Dynamic Island State (Simulating physical Dynamic Island hardware interactions)
    var isIslandExpanded by remember { mutableStateOf(false) }
    var muteAlerts by remember { mutableStateOf(false) }

    // Dialog state for adding a new city
    var showAddCityDialog by remember { mutableStateOf(false) }
    var newCityName by remember { mutableStateOf("") }
    var newCityRegion by remember { mutableStateOf("Miền Bắc") }
    var newCityLat by remember { mutableStateOf("") }
    var newCityLon by remember { mutableStateOf("") }

    // Chat custom question state
    var customQuestionInput by remember { mutableStateOf("") }

    val filteredCities = if (searchQuery.isEmpty()) {
        allCities
    } else {
        allCities.filter { 
            it.cityName.lowercase().contains(searchQuery.lowercase()) ||
            it.region.lowercase().contains(searchQuery.lowercase())
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F2027),
                        Color(0xFF203A43),
                        Color(0xFF2C5364)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(72.dp)) // Space for Floating Dynamic Island

            // --- App Header ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Thời Tiết Việt",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Dự báo thời tiết & Trợ lý thông minh",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                IconButton(
                    onClick = { showAddCityDialog = true },
                    modifier = Modifier.background(Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddLocation,
                        contentDescription = "Thêm địa điểm mới",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Search Field ---
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Tìm kiếm tỉnh thành, vùng miền...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Tìm kiếm") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Xoá tìm kiếm")
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF64B5F6),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedContainerColor = Color.White.copy(alpha = 0.08f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
                    focusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
                    unfocusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                    focusedLeadingIconColor = Color.White,
                    unfocusedLeadingIconColor = Color.White.copy(alpha = 0.6f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- Multi-location slider ---
            Text(
                text = "Các địa điểm giám sát",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filteredCities) { city ->
                    val isSelected = activeCity?.id == city.id
                    Card(
                        modifier = Modifier
                            .width(130.dp)
                            .height(78.dp)
                            .clickable { viewModel.selectCity(city) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF0288D1).copy(alpha = 0.9f) else Color.White.copy(alpha = 0.08f)
                        ),
                        border = if (isSelected) BorderStroke(1.5.dp, Color(0xFF64B5F6)) else null,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = city.cityName,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = if (city.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Yêu thích",
                                    tint = if (city.isFavorite) Color.Red else Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { viewModel.toggleFavorite(city) }
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = city.region,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = if (city.lastUpdated > 0L) "${city.lastTemp.toInt()}°C" else "--°C",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- Active Weather Layout ---
            if (activeCity != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(24.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${activeCity.cityName} • ${activeCity.region}",
                                color = Color.White,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val dateStr = if (activeCity.lastUpdated > 0L) {
                                val sdf = SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault())
                                "Cập nhật lúc: " + sdf.format(Date(activeCity.lastUpdated))
                            } else {
                                "Chưa có kết nối thời tiết"
                            }
                            Text(
                                text = dateStr,
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }

                        IconButton(
                            onClick = { viewModel.refreshWeatherData(activeCity) },
                            modifier = Modifier.background(Color.White.copy(alpha = 0.12f), CircleShape)
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Làm mới dự báo", tint = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Huge Temp Display with Meteorologic Details
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        getWeatherIcon(activeCity.lastConditionCode)?.let { icon ->
                            Icon(
                                imageVector = icon,
                                contentDescription = activeCity.lastConditionText,
                                tint = Color(0xFFFBC02D),
                                modifier = Modifier.size(72.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = if (activeCity.lastUpdated > 0L) "${activeCity.lastTemp.toInt()}°C" else "--",
                            fontSize = 68.sp,
                            fontWeight = FontWeight.Light,
                            color = Color.White
                        )
                    }

                    Text(
                        text = activeCity.lastConditionText,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Atmosphere Factors Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        AtmosphereIndicator(
                            icon = Icons.Default.WaterDrop,
                            title = "Độ ẩm không khí",
                            value = "74%",  // Simulated local humidity standard
                            tint = Color(0xFF64B5F6)
                        )
                        AtmosphereIndicator(
                            icon = Icons.Default.Air,
                            title = "Vận tốc Gió",
                            value = "12 km/h",
                            tint = Color(0xFFA1887F)
                        )
                        AtmosphereIndicator(
                            icon = Icons.Default.LocationOn,
                            title = "Tọa độ địa lý",
                            value = "${String.format("%.2f", activeCity.latitude)}°, ${String.format("%.2f", activeCity.longitude)}°",
                            tint = Color(0xFF81C784)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // --- Gemini Intelligent Section ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF152238).copy(alpha = 0.85f)),
                    border = BorderStroke(1.dp, Color(0xFF64B5F6).copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFF03A9F4), CircleShape)
                                    .wrapContentSize(Alignment.Center)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Lời khuyên từ Trợ lý Trí Tuệ Nhân Tạo",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Advice bubble
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            if (aiLoading) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF03A9F4), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Thời Tiết Việt AI đang phân tích dữ liệu...",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 13.sp
                                    )
                                }
                            } else {
                                Text(
                                    text = if (geminiAdvice.isNotEmpty()) geminiAdvice else "Vui lòng chọn hoặc làm mới thời tiết để nhận lời khuyên tự động từ AI về vị thế khí hậu của thành phố.",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Suggestion questions
                        Text(
                            text = "Hỏi Trợ lý Thời tiết về khu vực ${activeCity.cityName}:",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        // Sample chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(viewModel.sampleQuestions) { question ->
                                SuggestionChip(
                                    label = { Text(question, color = Color.White, fontSize = 11.sp) },
                                    onClick = { viewModel.askCustomQuestion(question) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = Color.White.copy(alpha = 0.1f)
                                    ),
                                    border = null,
                                    modifier = Modifier.height(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Custom Query input row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customQuestionInput,
                                onValueChange = { customQuestionInput = it },
                                placeholder = { Text("Hỏi thêm AI về khí hậu, trang phục...", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF03A9F4),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedContainerColor = Color.White.copy(alpha = 0.05f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                                    unfocusedPlaceholderColor = Color.White.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (customQuestionInput.isNotBlank()) {
                                        viewModel.askCustomQuestion(customQuestionInput)
                                        customQuestionInput = ""
                                    }
                                },
                                enabled = customQuestionInput.isNotBlank() && !aiLoading,
                                modifier = Modifier.background(
                                    if (customQuestionInput.isNotBlank()) Color(0xFF03A9F4) else Color.White.copy(alpha = 0.1f),
                                    RoundedCornerShape(12.dp)
                                )
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Gửi", tint = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        // --- Simulated Dynamic Island Notch Simulation Overlay (Floating Head) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(26.dp))
                    .border(
                        width = 1.2.dp,
                        color = if (isIslandExpanded) Color(0xFF64B5F6).copy(alpha = 0.4f) else Color.Transparent,
                        shape = RoundedCornerShape(26.dp)
                    )
                    .background(Color.Black)
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = 0.85f,
                            stiffness = 200f
                        )
                    )
                    .clickable { isIslandExpanded = !isIslandExpanded }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                if (!isIslandExpanded) {
                    // --- COMPACT ISLAND VIEW ---
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (activeCity?.lastConditionCode ?: 0 >= 60) Icons.Default.Warning else Icons.Default.CloudQueue,
                            contentDescription = "Dynamic Island Icon",
                            tint = if (activeCity?.lastConditionCode ?: 0 >= 60) Color.Red else Color(0xFF03A9F4),
                            modifier = Modifier.size(18.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier
                                .height(16.dp)
                                .width(1.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        )
                        Text(
                            text = if (activeCity != null) "${activeCity.cityName}: ${activeCity.lastTemp.toInt()}°C" else "Thời Tiết Việt",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (activeCity?.lastConditionCode ?: 0 >= 60 && !muteAlerts) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color.Red, CircleShape)
                            )
                        }
                    }
                } else {
                    // --- EXPANDED ISLAND VIEW ---
                    Column(
                        modifier = Modifier.width(310.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FlashOn,
                                    contentDescription = "Live Activity",
                                    tint = Color.Yellow,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "LIVE WEATHER ISLAND (A16)",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.1.sp
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Thu nhỏ",
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { isIslandExpanded = false }
                            )
                        }

                        if (activeCity != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = activeCity.cityName,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Nhiệt độ hiện tại: ${activeCity.lastTemp.toInt()}°C",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = activeCity.lastConditionText,
                                        color = Color(0xFF03A9F4),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                getWeatherIcon(activeCity.lastConditionCode)?.let { icon ->
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = Color(0xFFFBC02D),
                                        modifier = Modifier.size(42.dp)
                                    )
                                }
                            }

                            // Dynamic Warning Alert inside Dynamic Island (simulating system severity alerts)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (activeCity.lastConditionCode >= 60) Color.Red.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (activeCity.lastConditionCode >= 60) Icons.Default.Warning else Icons.Default.Info,
                                        contentDescription = "Thông báo cảnh báo",
                                        tint = if (activeCity.lastConditionCode >= 60) Color.Red else Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (activeCity.lastConditionCode >= 60) {
                                            "Cảnh báo: Thời tiết có mưa bão lớn dông lốc, hạn chế ra đường!"
                                        } else {
                                            "Khí hậu hôm nay ôn hòa, thích hợp đi dã ngoại làm việc."
                                        },
                                        color = Color.White,
                                        fontSize = 9.5.sp,
                                        lineHeight = 12.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // Interactive toggles inside Expanded Dynamic Island
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.refreshWeatherData(activeCity) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(32.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Làm mới", fontSize = 10.sp, color = Color.White)
                                }

                                Button(
                                    onClick = { muteAlerts = !muteAlerts },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (muteAlerts) Color.White.copy(alpha = 0.15f) else Color.Red.copy(alpha = 0.3f)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (muteAlerts) Icons.Default.NotificationsOff else Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (muteAlerts) "Bật âm" else "Tắt báo", fontSize = 10.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOG FOR ADDING A NEW CUSTOM LOCATION IN VIETNAM ---
    if (showAddCityDialog) {
        AlertDialog(
            onDismissRequest = { showAddCityDialog = false },
            title = { Text("Thêm vị thế thời tiết mới") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newCityName,
                        onValueChange = { newCityName = it },
                        label = { Text("Tên tỉnh/thành phố") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Vùng miền:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("Miền Bắc", "Miền Trung", "Miền Nam", "Tây Nguyên").forEach { r ->
                            val rSelected = newCityRegion == r
                            FilterChip(
                                selected = rSelected,
                                onClick = { newCityRegion = r },
                                label = { Text(r, fontSize = 10.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = newCityLat,
                        onValueChange = { newCityLat = it },
                        label = { Text("Vĩ độ (Latitude, VD: 16.0)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newCityLon,
                        onValueChange = { newCityLon = it },
                        label = { Text("Kinh độ (Longitude, VD: 108.0)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val latVal = newCityLat.toDoubleOrNull() ?: 21.0
                        val lonVal = newCityLon.toDoubleOrNull() ?: 105.0
                        if (newCityName.isNotBlank()) {
                            viewModel.addNewLocation(
                                name = newCityName,
                                region = newCityRegion,
                                lat = latVal,
                                lon = lonVal
                            )
                            newCityName = ""
                            newCityLat = ""
                            newCityLon = ""
                            showAddCityDialog = false
                        }
                    }
                ) {
                    Text("Thêm ngay")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCityDialog = false }) {
                    Text("Huỷ bỏ")
                }
            }
        )
    }
}

@Composable
fun AtmosphereIndicator(
    icon: ImageVector,
    title: String,
    value: String,
    tint: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 9.5.sp
        )
    }
}

fun getWeatherIcon(code: Int): ImageVector? {
    return when (code) {
        0 -> Icons.Default.WbSunny
        1, 2 -> Icons.Default.WbCloudy
        3 -> Icons.Default.Cloud
        45, 48 -> Icons.Default.BlurOn
        51, 53, 55 -> Icons.Default.Grain
        61, 63 -> Icons.Default.Opacity
        65 -> Icons.Default.BeachAccess
        71, 73, 75 -> Icons.Default.AcUnit
        80, 81, 82 -> Icons.Default.Umbrella
        95, 96, 99 -> Icons.Default.Thunderstorm
        else -> Icons.Default.CloudQueue
    }
}
