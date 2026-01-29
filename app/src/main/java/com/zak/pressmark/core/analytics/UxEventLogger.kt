package com.zak.pressmark.core.analytics

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

interface UxEventLogger {
    fun logEvent(name: String, params: Map<String, Any?> = emptyMap())
}

@Singleton
class LogcatUxEventLogger @Inject constructor() : UxEventLogger {
    override fun logEvent(name: String, params: Map<String, Any?>) {
        val payload = if (params.isEmpty()) {
            ""
        } else {
            params.entries.joinToString(prefix = " ", separator = " ") { (key, value) ->
                "$key=${value ?: "null"}"
            }
        }
        Log.i("PressmarkUx", "$name$payload")
    }
}
