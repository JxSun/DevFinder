package com.jxsun.devfinder.data.remote

import com.google.gson.annotations.SerializedName

data class GitHubResponse<T>(
        @SerializedName("total_count") val total: Int,
        @SerializedName("incomplete_results") val incomplete: Boolean,
        @SerializedName("items") val items: List<T>
)