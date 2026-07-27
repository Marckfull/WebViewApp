package com.chuvadeletras.game.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * O SDK de anúncios precisa da Activity, e o Context que o Compose entrega pode
 * ser um wrapper. Desembrulha até achar a Activity — ou null, se não houver.
 */
fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
