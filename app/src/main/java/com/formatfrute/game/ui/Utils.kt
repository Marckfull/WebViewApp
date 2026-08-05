package com.formatfrute.game.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

fun formatScore(value: Int): String = when {
    value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000f)
    value >= 10_000 -> String.format("%.1fk", value / 1000f)
    else -> value.toString()
}

fun formatClock(seconds: Int): String =
    "%d:%02d".format(seconds / 60, seconds % 60)
