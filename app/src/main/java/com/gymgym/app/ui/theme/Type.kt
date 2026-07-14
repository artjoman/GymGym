package com.gymgym.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.gymgym.app.R

/** Rajdhani — condensed athletic display face, for headings, buttons, and numbers. */
val Rajdhani = FontFamily(
    Font(R.font.rajdhani_medium, FontWeight.Medium),
    Font(R.font.rajdhani_semibold, FontWeight.SemiBold),
    Font(R.font.rajdhani_bold, FontWeight.Bold),
)

/** Barlow — clean grotesque, for body text and labels. */
val Barlow = FontFamily(
    Font(R.font.barlow_regular, FontWeight.Normal),
    Font(R.font.barlow_medium, FontWeight.Medium),
    Font(R.font.barlow_semibold, FontWeight.SemiBold),
)

val GymTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Rajdhani, fontWeight = FontWeight.Bold, fontSize = 48.sp, letterSpacing = 0.5.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Rajdhani, fontWeight = FontWeight.Bold, fontSize = 30.sp, letterSpacing = 0.5.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = Rajdhani, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, letterSpacing = 0.3.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Rajdhani, fontWeight = FontWeight.SemiBold, fontSize = 22.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Rajdhani, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, letterSpacing = 0.2.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Barlow, fontWeight = FontWeight.Normal, fontSize = 16.sp, letterSpacing = 0.2.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Barlow, fontWeight = FontWeight.Normal, fontSize = 14.sp, letterSpacing = 0.2.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Barlow, fontWeight = FontWeight.Normal, fontSize = 12.sp, letterSpacing = 0.2.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Rajdhani, fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Barlow, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.5.sp,
    ),
)
