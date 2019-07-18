package com.jxsun.devfinder.feature

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.jxsun.devfinder.R
import com.jxsun.devfinder.util.extention.transaction

class DevListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_devlist)

        supportFragmentManager.transaction {
            it.replace(R.id.contentFrame, DevListFragment.newInstance(), DevListFragment.TAG)
        }
    }
}