package com.eyeson.android.ui.theme

import androidx.compose.material.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.eyeson.android.R

private val OpenSans = FontFamily(
    Font(R.font.opensans_regular, FontWeight.Normal),
    Font(R.font.opensans_bold, FontWeight.Bold),
    Font(R.font.opensans_semibold, FontWeight.SemiBold)
)


val Typography = Typography(
    defaultFontFamily = OpenSans,
    h1 = TextStyle(
        fontFamily = OpenSans,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        letterSpacing = 2.sp
    ),
    body1 = TextStyle(
        fontFamily = OpenSans,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        letterSpacing = 0.sp,
        lineHeight = 25.sp
    ),
    button = TextStyle(
        fontFamily = OpenSans,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 16.sp,
        letterSpacing = 1.sp
    ),
    caption = TextStyle(
        fontFamily = OpenSans,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    overline = TextStyle(
        fontFamily = OpenSans,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 16.sp,
        letterSpacing = 1.sp
    )
)
