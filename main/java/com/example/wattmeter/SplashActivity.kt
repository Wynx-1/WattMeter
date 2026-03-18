package com.example.wattmeter

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val tvAppName = findViewById<TextView>(R.id.tvAppName)

        // Start invisible and small
        tvAppName.alpha = 0f
        tvAppName.scaleX = 0.85f
        tvAppName.scaleY = 0.85f
        tvAppName.translationY = 30f

        // Animate in — fade, scale up, move up
        tvAppName.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .translationY(0f)
            .setDuration(1200)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                // Fade out after showing
                tvAppName.animate()
                    .alpha(0f)
                    .scaleX(1.08f)
                    .scaleY(1.08f)
                    .setDuration(600)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                    .start()
            }
            .start()
    }
}