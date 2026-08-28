package com.tc128.giamdinhnative.di

import com.tc128.giamdinhnative.data.remote.ApiService
import com.tc128.giamdinhnative.session.SessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://tc128hp.hopto.org/"

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(sessionManager: SessionManager): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            // NAS qua Tailscale/hopto chậm nhưng KHÔNG để quá lâu: timeout 120s khiến 1 ảnh treo tới
            // 120s, 1 lô 10 ảnh treo ~20 phút → worker giữ KEEP-lock lâu (nút Upload thành no-op) rồi
            // bị cắt. 60s đủ cho ảnh ~1MB tới NAS chậm, mà phát hiện treo nhanh hơn để worker retry sớm.
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)
            // Tắt tự-retry của OkHttp: với POST upload (không idempotent), nếu kết nối rớt sau khi
            // server đã nhận ảnh, OkHttp retry sẽ gửi lại → tạo bản trùng ở backend. Tắt để mỗi ảnh
            // chỉ gửi đúng 1 lần cho mỗi lần worker chạy.
            .retryOnConnectionFailure(false)
            .addInterceptor(HttpLoggingInterceptor().apply {
                // HEADERS thay vì BODY: BODY log toàn bộ bytes ảnh mỗi lần upload → chậm + tốn RAM,
                // góp phần làm upload lâu. HEADERS đủ để chẩn đoán mà không đọc cả ảnh vào log.
                level = HttpLoggingInterceptor.Level.HEADERS
            })
            .addInterceptor { chain ->
                val token = runBlocking { sessionManager.getToken() }
                val request = chain.request().newBuilder()
                    .apply { if (token != null) addHeader("Authorization", "Bearer $token") }
                    // Đánh dấu request từ app native — backend dùng header này để phân biệt
                    // với app Xamarin cũ (gọi cùng endpoint api/containerv2 nhưng không có header)
                    .addHeader("X-Client-App", "GiamDinhNative")
                    .build()
                val response = chain.proceed(request)
                // Server redirect về trang login (HTML) khi token hết hạn
                // → chuyển thành 401 cho Retrofit xử lý, KHÔNG xóa token ở đây
                // (token xóa khi user thao tác logout, không phải khi bất kỳ request lỗi)
                val isHtml = response.header("content-type")?.contains("text/html") == true
                if (isHtml) {
                    response.close()
                    okhttp3.Response.Builder()
                        .request(request)
                        .protocol(okhttp3.Protocol.HTTP_1_1)
                        .code(401)
                        .message("Unauthorized")
                        .body("Phiên đăng nhập hết hạn".toResponseBody("text/plain".toMediaType()))
                        .build()
                } else {
                    response
                }
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json; charset=UTF-8".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService =
        retrofit.create(ApiService::class.java)
}
