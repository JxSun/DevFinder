package com.jxsun.devfinder.util

import android.content.Context
import android.net.ConnectivityManager

class NetworkChecker(
        private val context: Context
) {

    fun isNetworkConnected(): Boolean {
        val networkInfo = context.getSystemService(ConnectivityManager::class.java)
                .activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }
}