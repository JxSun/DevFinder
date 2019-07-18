package com.jxsun.devfinder.data.remote

import com.jxsun.devfinder.BuildConfig
import io.reactivex.Single
import retrofit2.adapter.rxjava2.Result
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
}