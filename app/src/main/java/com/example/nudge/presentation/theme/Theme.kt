package com.example.nudge.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.MaterialTheme

@Composable
fun NudgeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        content = content
    )
}