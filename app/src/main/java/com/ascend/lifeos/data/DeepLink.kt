package com.ascend.lifeos.data

import androidx.compose.runtime.mutableStateOf

/** Pending navigation from notifications/widgets — consumed once by the shell. */
object DeepLink {
    val pending = mutableStateOf<String?>(null) // "train" | "fuel" | "body" | "report" | null
    fun consume(): String? = pending.value.also { pending.value = null }
}
