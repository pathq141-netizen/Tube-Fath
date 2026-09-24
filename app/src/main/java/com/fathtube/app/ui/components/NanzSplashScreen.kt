package com.fathtube.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fathtube.app.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Netflix-style Electric Blue cinematic palette
private val OledBlack = Color(0xFF000000)
private val DeepBlueGlow = Color(0xFF001535)
private val MidnightBlue = Color(0xFF000A1A)
private val ElectricBlue = Color(0xFF0070F3)
private val NeonCyanBlue = Color(0xFF00D2FF)
private val DeepSapphire = Color(0xFF0044B3)

/**
 * 100% Netflix-style Cinematic Intro Splash Screen (Duration: exactly 3.0 seconds).
 *
 * Choreography (3000ms):
 * - Letters fly in from alternating sides and gather in the center:
 *   "Nanz" on top row (Electric Blue & Neon Cyan glow)
 *   "Tube" on bottom row (Crisp White & Ice Cyan glow)
 * - Laser ribbon bursts across the center as letters assemble.
 * - 3D glossy badge appears above with gentle glow pulse.
 * - Grand Netflix crescendo zoom-through and seamless dissolve into main app at exactly 3.0s.
 */
@Composable
fun NanzSplashScreen(
    onAnimationFinished: () -> Unit
) {
    // Letters data: "Nanz" on top row, "Tube" on bottom row
    val nanzLetters = remember { listOf('F', 'A', 'T', 'H') }
    val tubeLetters = remember { listOf('T', 'u', 'b', 'e') }

    // Individual letter animatables (0f = off-screen side, 1f = centered in place)
    val nanzAnimatables = remember { List(nanzLetters.size) { Animatable(0f) } }
    val tubeAnimatables = remember { List(tubeLetters.size) { Animatable(0f) } }

    // Master container controllers
    val screenAlpha = remember { Animatable(1f) }
    val masterScale = remember { Animatable(0.92f) }
    val badgeAlpha = remember { Animatable(0f) }
    val badgeScale = remember { Animatable(0.6f) }
    val ribbonProgress = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }

    // Ambient breathing blue pulse
    val infiniteTransition = rememberInfiniteTransition(label = "BlueGlow")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowPulse"
    )

    // --- Exactly 3.0s (3000ms) Netflix Choreography ---
    LaunchedEffect(Unit) {
        // 1. Badge reveals smoothly
        launch {
            delay(100)
            badgeAlpha.animateTo(1f, tween(durationMillis = 600, easing = FastOutSlowInEasing))
        }
        launch {
            delay(100)
            badgeScale.animateTo(1f, tween(durationMillis = 750, easing = CubicBezierEasing(0.18f, 1f, 0.3f, 1f)))
        }

        // 2. "Nanz" letters fly in from alternating sides (left / right)
        nanzAnimatables.forEachIndexed { index, anim ->
            launch {
                delay(120L + index * 65L)
                anim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 600,
                        easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
                    )
                )
            }
        }

        // 3. "Tube" letters fly in from alternating sides with slight offset
        tubeAnimatables.forEachIndexed { index, anim ->
            launch {
                delay(260L + index * 65L)
                anim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 600,
                        easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
                    )
                )
            }
        }

        // 4. Ribbon laser flare sweeps across center once letters start locking in
        launch {
            delay(700)
            ribbonProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
            )
        }

        // 5. Footer developer attribution fades in
        launch {
            delay(1100)
            subtitleAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 600, easing = LinearEasing)
            )
        }

        // 6. Camera anamorphic creep: gentle continuous zoom from 0.92 to 1.05
        launch {
            masterScale.animateTo(
                targetValue = 1.05f,
                animationSpec = tween(durationMillis = 2400, easing = LinearEasing)
            )
        }

        // 7. Climax: Netflix crescendo zoom-through & dissolve into app at 2400ms -> 3000ms
        launch {
            delay(2400)
            launch {
                masterScale.animateTo(
                    targetValue = 1.6f,
                    animationSpec = tween(durationMillis = 600, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f))
                )
            }
            launch {
                screenAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
                )
            }
        }

        // End of 3.0 seconds (3000ms): hand off to main app
        delay(3000)
        onAnimationFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .graphicsLayer { alpha = screenAlpha.value },
        contentAlignment = Alignment.Center
    ) {
        // --- 1. Ambient Radial Atmospheric Blue Glow ---
        Box(
            modifier = Modifier
                .size(480.dp)
                .scale(masterScale.value)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ElectricBlue.copy(alpha = 0.32f * glowPulse),
                            DeepBlueGlow.copy(alpha = 0.20f * glowPulse),
                            MidnightBlue.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // --- 2. Master Animated Stage (Anamorphic zoom & typography) ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = masterScale.value
                    scaleY = masterScale.value
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                // 1. 3D Glossy Badge of FathTube
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .scale(badgeScale.value)
                        .alpha(badgeAlpha.value),
                    contentAlignment = Alignment.Center
                ) {
                    // Soft cyan halo ring
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(NeonCyanBlue.copy(alpha = 0.35f * glowPulse), Color.Transparent)
                                )
                            )
                    )

                    Image(
                        painter = painterResource(id = R.drawable.ic_splash_logo),
                        contentDescription = "FathTube",
                        modifier = Modifier.size(80.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 2. Row 1: "Nanz" (di atas) - Huruf meluncur dari samping berkumpul di tengah
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    nanzLetters.forEachIndexed { index, char ->
                        val progress = nanzAnimatables[index].value
                        val fromLeft = index % 2 == 0
                        val startOffset = if (fromLeft) -360f else 360f
                        val currentOffsetX = (1f - progress) * startOffset
                        val currentScale = 1.7f - (0.7f * progress)
                        val currentRotation = (1f - progress) * (if (fromLeft) -22f else 22f)

                        Text(
                            text = char.toString(),
                            fontSize = 44.sp,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Black,
                            color = ElectricBlue,
                            modifier = Modifier.graphicsLayer {
                                translationX = currentOffsetX
                                alpha = progress
                                scaleX = currentScale
                                scaleY = currentScale
                                rotationZ = currentRotation
                                shadowElevation = 24f * progress
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 3. The Iconic Laser Ribbon Flare in Electric Blue & Cyan
                Box(
                    modifier = Modifier
                        .width(220.dp)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(Color(0x220070F3)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(ribbonProgress.value)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        DeepSapphire,
                                        ElectricBlue,
                                        NeonCyanBlue,
                                        Color.White,
                                        NeonCyanBlue,
                                        ElectricBlue,
                                        DeepSapphire,
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 4. Row 2: "Tube" (di bawah) - Huruf meluncur dari samping berkumpul di tengah
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tubeLetters.forEachIndexed { index, char ->
                        val progress = tubeAnimatables[index].value
                        val fromLeft = index % 2 != 0
                        val startOffset = if (fromLeft) -400f else 400f
                        val currentOffsetX = (1f - progress) * startOffset
                        val currentScale = 1.6f - (0.6f * progress)
                        val currentRotation = (1f - progress) * (if (fromLeft) -18f else 18f)

                        Text(
                            text = char.toString(),
                            fontSize = 36.sp,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 4.sp,
                            color = Color.White,
                            modifier = Modifier.graphicsLayer {
                                translationX = currentOffsetX
                                alpha = progress
                                scaleX = currentScale
                                scaleY = currentScale
                                rotationZ = currentRotation
                                shadowElevation = 18f * progress
                            }
                        )
                    }
                }
            }

            // 5. Subtle Footer "Dev oleh Fath (ALfath)" at bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 44.dp)
                    .alpha(subtitleAlpha.value)
            ) {
                Text(
                    text = "Dev oleh Fath (ALfath)",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}
