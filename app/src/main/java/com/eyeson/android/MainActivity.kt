package com.eyeson.android

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.eyeson.android.ui.theme.EyesonDemoTheme
import com.eyeson.android.ui.view.connection.ConnectionFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val compose = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (compose) {
            enableEdgeToEdge()

            setContent {
                EyesonDemoTheme {
                    EyesonDemoNavHost()
                }
            }
        } else {
            /*
            * Basic example that shows how to use eyeson SDK in the view world.
            */

            setContentView(R.layout.main_activity)
            if (savedInstanceState == null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.container, ConnectionFragment.newInstance())
                    .commitNow()
            }
        }

    }
}