package com.ascend.lifeos.data

import androidx.compose.runtime.mutableStateOf

/** Pending navigation from notifications/widgets — consumed once by the shell. */
object DeepLink {
    val pending = mutableStateOf<String?>(null)
    private val atom = java.util.concurrent.atomic.AtomicReference<String?>(null)
    fun set(v: String) { atom.set(v); pending.value = v }
    fun consume(): String? = atom.getAndSet(null).also { pending.value = null }
}
