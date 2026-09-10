package com.khaled.frais.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.khaled.frais.features.activity.ActiveAppViewModel
import com.khaled.frais.ui.FraisMainUI
import com.khaled.frais.ui.home.viewmodel.HomeViewModel

class MainActivity : AppCompatActivity() {
    private val homeViewModel: HomeViewModel by viewModels()
    private val activeAppViewModel: ActiveAppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            me.zhanghai.compose.preference.ProvidePreferenceLocals {
                FraisMainUI(homeViewModel = homeViewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        activeAppViewModel.refresh()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.hasCategory(Intent.CATEGORY_HOME) == true) {
            homeViewModel.triggerGoHome()
        }
    }

    override fun onStop() {
        super.onStop()
        com.khaled.frais.utils.HIcon.applyPendingIconState()
    }
}
