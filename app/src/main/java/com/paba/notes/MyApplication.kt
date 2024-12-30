package com.paba.notes

import android.app.Application
import com.google.firebase.FirebaseApp


class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize FirebaseApp with the application context
        FirebaseApp.initializeApp(this)
    }
}