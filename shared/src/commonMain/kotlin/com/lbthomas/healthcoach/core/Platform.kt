
package com.lbthomas.healthcoach.core

import androidx.compose.runtime.Composable

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class Platform {
    val name: String
}

expect fun getPlatform(): Platform

@Composable
expect fun getPlatformContext(): Any?