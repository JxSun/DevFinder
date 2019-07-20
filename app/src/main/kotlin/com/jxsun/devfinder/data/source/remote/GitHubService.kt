package com.jxsun.devfinder.data.source.remote

import com.jxsun.devfinder.BuildConfig
import io.reactivex.Single
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.Result
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface GitHubService {

    @GET("search/users")
    fun getUsers(
            @Query("q") query: String,
            @Query("page") page: Int,
            @Query("client_id") clientId: String = BuildConfig.CLIENT_ID,
            @Query("client_secret") clientSecret: String = BuildConfig.CLIENT_SECRET
    ): Single<Result<GitHubResponse<UserResponse>>>

    class Factory {

        fun create(customOkHttpClient: OkHttpClient? = null): GitHubService {
            return Retrofit.Builder()
                    .baseUrl("https://api.github.com")
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .client(customOkHttpClient ?: createDefaultOkHttpClient())
                    .build()
                    .create(GitHubService::class.java)
        }

        private fun createDefaultOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                    .apply {
                        if (BuildConfig.DEBUG) {
                            val loggingInterceptor = HttpLoggingInterceptor().apply {
                                level = HttpLoggingInterceptor.Level.BODY
                            }
                            addInterceptor(loggingInterceptor)
                        }
                    }
                    .build()
        }
    }
}