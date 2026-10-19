package com.amasesoft.tabatatimer.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Huge countdown digits; tabular figures keep the width stable while counting. */
val CountdownTextStyle = TextStyle(
    fontSize = 112.sp,
    lineHeight = 112.sp,
    fontWeight = FontWeight.Bold,
    fontFeatureSettings = "tnum",
)

/** Tabular figures for any changing number (steppers, totals). */
val TabularNumbers = TextStyle(fontFeatureSettings = "tnum")
