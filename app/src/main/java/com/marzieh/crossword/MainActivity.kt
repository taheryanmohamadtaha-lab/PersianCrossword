package com.marzieh.crossword

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    private lateinit var app: App

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = App(applicationContext, this)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { app.onBack() }
        })
        setContent { AppRoot(app) }
    }

    override fun onPause() { super.onPause(); app.onPause() }
    override fun onResume() { super.onResume(); app.onResume() }
    override fun onDestroy() { super.onDestroy(); if (isFinishing) app.destroy() }
}
