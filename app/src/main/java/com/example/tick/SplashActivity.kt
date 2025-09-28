package com.example.tick

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.airbnb.lottie.compose.*

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SplashScreen(
                onAnimationFinished = {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            )
        }
    }
}

@Composable
fun SplashScreen(onAnimationFinished: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(contentAlignment = Alignment.Center) {
            // Load the animation
            val composition = rememberLottieComposition(
                LottieCompositionSpec.RawRes(R.raw.tick)
            ).value

            // Animate it
            val progress = animateLottieCompositionAsState(
                composition = composition,
                iterations = 1
            ).value

            if (composition != null) {
                LottieAnimation(
                    composition = composition,
                    progress = { progress }
                )
            }

            // Run only once when animation completes
            LaunchedEffect(progress) {
                if (progress == 1f) {
                    onAnimationFinished()
                }
            }
        }
    }
}
