
package com.lbthomas.healthcoach.core

import androidx.compose.runtime.Composable

expect class Platform {
    val name: String
}

expect fun getPlatform(): Platform

@Composable
expect fun getPlatformContext(): Any?