package com.jxsun.devfinder.util

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jxsun.devfinder.feature.DevListViewModel

class ViewModelFactory(
        private val appContext: Context
) : ViewModelProvider.Factory {

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: ViewModelFactory? = null

        fun getInstance(context: Context) =
                INSTANCE ?: synchronized(ViewModelFactory::class.java) {
                    INSTANCE ?: ViewModelFactory(context)
                            .also { INSTANCE = it }
                }


        @VisibleForTesting
        fun destroyInstance() {
            INSTANCE = null
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return with(modelClass) {
            when {
                isAssignableFrom(DevListViewModel::class.java) -> {
                    DevListViewModel(
                            Injection.getDevListActionProcessor()
                    ) as T
                }
                else -> throw IllegalArgumentException("ViewModel not found: ${modelClass.name}")
            }
        }
    }
}