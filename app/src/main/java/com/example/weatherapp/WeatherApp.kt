package com.example.weatherapp

import android.app.Application
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class WeatherApp : Application() {

    private val FLAGS = FLAG_ACTIVITY_SINGLE_TOP or
            FLAG_ACTIVITY_NEW_TASK or
            FLAG_ACTIVITY_CLEAR_TASK

    private var isLoggedIn: Boolean? = null

    override fun onCreate() {
        super.onCreate()

        Firebase.auth.addAuthStateListener { firebaseAuth ->
            val loggedIn = firebaseAuth.currentUser != null
            if (isLoggedIn == loggedIn) return@addAuthStateListener
            isLoggedIn = loggedIn
            if (loggedIn) {
                goToMain()
            } else {
                goToLogin()
            }
        }
    }

    private fun goToMain() {
        startActivity(
            Intent(this, MainActivity::class.java).setFlags(FLAGS)
        )
    }

    private fun goToLogin() {
        startActivity(
            Intent(this, LoginActivity::class.java).setFlags(FLAGS)
        )
    }
}