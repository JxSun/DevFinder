package com.jxsun.devfinder.data.remote

import com.jxsun.devfinder.BuildConfig
import io.reactivex.Single
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.Result
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

class GitHubServiceImpl : GitHubService {

    private val service: GitHubService by lazy<GitHubService>(LazyThreadSafetyMode.SYNCHRONIZED) {
        Retrofit.Builder()
                .baseUrl("https://api.github.com")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(createOkHttpClient())
                .build()
                .create(GitHubService::class.java)
    }

    private fun createOkHttpClient(): OkHttpClient {
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

    override fun getUsers(
            query: String,
            page: Int,
            clientId: String,
            clientSecret: String
    ): Single<Result<GitHubResponse<UserResponse>>> {
        return service.getUsers(query, page, clientId, clientSecret)
    }
}