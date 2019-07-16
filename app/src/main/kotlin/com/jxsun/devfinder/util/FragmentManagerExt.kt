package com.jxsun.devfinder.util

import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction

inline fun FragmentManager.transaction(
        allowStateLoss: Boolean = true,
        block: (FragmentTransaction) -> Unit
) {
    beginTransaction().apply {
        block(this)
        if (allowStateLoss) {
            this.commitAllowingStateLoss()
        } else {
            this.commit()
        }
    }
}