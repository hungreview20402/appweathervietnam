package com.example.api

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null,
    val systemInstruction: GeminiContent? = null
)

data class GeminiContent(
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String
)

data class GeminiGenerationConfig(
    val temperature: Float? = null,
    @Json(name = "responseMimeType") val responseMimeType: String? = null
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

data class GeminiCandidate(
    val content: GeminiContent?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiServiceClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    suspend fun getAdvice(cityName: String, temp: Double, condition: String, question: String): String = withContext(Dispatchers.IO) {
        val apiKey = com.example.BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Trợ lý ảo thời tiết đang ngoại tuyến (Vui lòng thiết lập GEMINI_API_KEY hợp lệ trong bảng điều khiển Secrets để kích hoạt tính năng thông minh này)."
        }

        val prompt = "Yêu cầu: Vai trò là một Trợ lý Thời tiết Việt Nam thông thái và dí dỏm. Hãy trả lời câu hỏi: '$question' trong bối cảnh thời tiết tại $cityName hiện đang có nhiệt độ là ${temp}°C và điều kiện là '$condition'. Câu trả lời phải ngắn gọn, súc tích (khoảng 3-4 câu), đậm đà phong vị Việt Nam, mang tính khích lệ hoặc lời khuyên hữu ích về sức khỏe, trang phục hoặc đi lại."

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = prompt)))
            ),
            generationConfig = GeminiGenerationConfig(temperature = 0.7f),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = "Bạn là Trợ lý Thời tiết Việt (Thời Tiết Việt AI), luôn dùng tiếng Việt, vui tươi, am hiểu thời tiết các vùng miền Bắc, Trung, Nam, Tây Nguyên.")))
        )

        try {
            val response = service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "Thời Tiết Việt: Hiện chưa thể phân tích câu hỏi này, hãy thử lại sau nhé!"
        } catch (e: Exception) {
            "Thời Tiết Việt: Có lỗi xảy ra khi kết nối trợ lý AI (${e.localizedMessage}). Hãy kiểm tra kết nối mạng."
        }
    }
}
