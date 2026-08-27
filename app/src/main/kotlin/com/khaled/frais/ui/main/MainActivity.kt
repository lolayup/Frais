package com.khaled.frais.ui.main

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.khaled.frais.ui.FraisMainUI

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            me.zhanghai.compose.preference.ProvidePreferenceLocals {
                FraisMainUI()
            }
        }
    }
}
