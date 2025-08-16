package com.krishna.passwordstrengthener.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.krishna.passwordstrengthener.R

val bodyFontFamily =
    FontFamily(
        androidx.compose.ui.text.font.Font(
            resId = R.font.sf_pro_text_black,
            weight = FontWeight.Black,
        ),
        androidx.compose.ui.text.font.Font(
            resId = R.font.sf_pro_text_bold,
            weight = FontWeight.Bold,
        ),
        androidx.compose.ui.text.font.Font(
            resId = R.font.sf_pro_text_light,
            weight = FontWeight.Light,
        ),
        androidx.compose.ui.text.font.Font(
            resId = R.font.sf_pro_text_regular,
            weight = FontWeight.Normal,
        ),
        androidx.compose.ui.text.font.Font(
            resId = R.font.sf_pro_text_medium,
            weight = FontWeight.Medium,
        ),
        androidx.compose.ui.text.font.Font(
            resId = R.font.sf_pro_text_semibold,
            weight = FontWeight.SemiBold,
        ),
        androidx.compose.ui.text.font.Font(
            resId = R.font.sf_pro_text_thin,
            weight = FontWeight.Thin,
        ),
    )

// Default Material 3 typography values
val baseline = Typography()

val AppTypography =
    Typography(
        displayLarge = baseline.displayLarge.copy(fontFamily = bodyFontFamily),
        displayMedium = baseline.displayMedium.copy(fontFamily = bodyFontFamily),
        displaySmall = baseline.displaySmall.copy(fontFamily = bodyFontFamily),
        headlineLarge = baseline.headlineLarge.copy(fontFamily = bodyFontFamily),
        headlineMedium = baseline.headlineMedium.copy(fontFamily = bodyFontFamily),
        headlineSmall = baseline.headlineSmall.copy(fontFamily = bodyFontFamily),
        titleLarge = baseline.titleLarge.copy(fontFamily = bodyFontFamily),
        titleMedium = baseline.titleMedium.copy(fontFamily = bodyFontFamily),
        titleSmall = baseline.titleSmall.copy(fontFamily = bodyFontFamily),
        bodyLarge = baseline.bodyLarge.copy(fontFamily = bodyFontFamily),
        bodyMedium = baseline.bodyMedium.copy(fontFamily = bodyFontFamily),
        bodySmall = baseline.bodySmall.copy(fontFamily = bodyFontFamily),
        labelLarge = baseline.labelLarge.copy(fontFamily = bodyFontFamily),
        labelMedium = baseline.labelMedium.copy(fontFamily = bodyFontFamily),
        labelSmall = baseline.labelSmall.copy(fontFamily = bodyFontFamily),
    )
