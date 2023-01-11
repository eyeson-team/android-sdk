package com.eyeson.android

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import com.eyeson.android.ui.view.connection.ConnectionFragment
import com.google.accompanist.systemuicontroller.rememberSystemUiController

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        setContent {
            val systemUiController = rememberSystemUiController()
            SideEffect {
                systemUiController.setSystemBarsColor(Color.Black, darkIcons = false)
            }
            EyesonDemoNavHost()
        }
        return
        // view world
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, ConnectionFragment.newInstance())
                .commitNow()
        }
    }
}