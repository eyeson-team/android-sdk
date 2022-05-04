package com.eyeson.android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.eyeson.android.ui.connection.ConnectionFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, ConnectionFragment.newInstance())
                .commitNow()
        }
    }
}