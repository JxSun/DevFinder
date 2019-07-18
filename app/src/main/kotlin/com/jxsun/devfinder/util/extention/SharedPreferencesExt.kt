package com.jxsun.devfinder.util.extention

import android.annotation.SuppressLint
import android.content.SharedPreferences

@SuppressLint("ApplySharedPref")
inline fun SharedPreferences.edit(commit: Boolean = false, block: (SharedPreferences.Editor) -> Unit) {
    val editor = edit()
    block(editor)
    if (commit) {
        editor.commit()
    } else {
        editor.apply()
    }
}