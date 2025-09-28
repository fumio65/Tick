package com.example.tick.uidesign

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.tick.R

@Composable
fun SplashScreen(onAnimationFinished: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(contentAlignment = Alignment.Center) {
            val composition = rememberLottieComposition(
                LottieCompositionSpec.RawRes(R.raw.tick)
            ).value

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

            // Ensure navigation happens only once
            var hasNavigated by remember { mutableStateOf(false) }
            LaunchedEffect(progress) {
                if (progress == 1f && !hasNavigated) {
                    hasNavigated = true
                    onAnimationFinished()
                }
            }
        }
    }
}
