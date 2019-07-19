package com.jxsun.devfinder.data.local

import android.content.Context
import com.jxsun.devfinder.util.extention.edit

private const val PREFS_NAME = "com.jxsun.devfinder.prefs"
private const val KEY_KEYWORD = "KEY_KEYWORD"
private const val KEY_NEXT_PAGE = "KEY_NEXT_PAGE"
private const val KEY_MAX_PAGE = "KEY_MAX_PAGE"

class AppPreferences(
        private val context: Context
) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var keyword: String
        get() = preferences.getString(KEY_KEYWORD, "")
        set(value) {
            preferences.edit {
                it.putString(KEY_KEYWORD, value)
            }
        }

    var nextPage: Int
        get() = preferences.getInt(KEY_NEXT_PAGE, -1)
        set(value) {
            preferences.edit {
                it.putInt(KEY_NEXT_PAGE, value)
            }
        }

    var lastPage: Int
        get() = preferences.getInt(KEY_MAX_PAGE, -1)
        set(value) {
            preferences.edit {
                it.putInt(KEY_MAX_PAGE, value)
            }
        }
}