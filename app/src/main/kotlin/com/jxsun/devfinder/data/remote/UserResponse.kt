package com.jxsun.devfinder.data.remote

import com.google.gson.annotations.SerializedName

data class UserResponse(
        val id: Long,
        @SerializedName("login") val name: String,
        @SerializedName("avatar_url") val avatarUrl: String
)