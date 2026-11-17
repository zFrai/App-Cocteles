package com.barcraft.app.utils

import android.content.Context
import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

/**
 * Acceso a los textos de `res/values/strings.xml` desde fuera de las pantallas
 * (ViewModels, repositorios y modelos), donde no se puede usar `stringResource`.
 * Se inicializa una vez en [com.barcraft.app.BarCraftApplication].
 */
object Txt {
    private lateinit var res: Resources

    fun init(context: Context) {
        res = context.applicationContext.resources
    }

    fun get(@StringRes id: Int, vararg args: Any): String =
        if (args.isEmpty()) res.getString(id) else res.getString(id, *args)

    fun plural(@PluralsRes id: Int, count: Int, vararg args: Any): String =
        if (args.isEmpty()) res.getQuantityString(id, count, count) else res.getQuantityString(id, count, *args)
}
