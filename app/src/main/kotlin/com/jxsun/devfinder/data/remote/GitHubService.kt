package com.jxsun.devfinder.data.remote

import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query

interface GitHubService {

    @GET("search/users")
    fun getUsers(
            @Query("q") query: String
    ): Single<GitHubResponse<UserResponse>>
}