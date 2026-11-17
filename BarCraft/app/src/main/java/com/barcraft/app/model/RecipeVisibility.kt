package com.barcraft.app.model

import androidx.annotation.StringRes
import com.barcraft.app.R
import com.barcraft.app.utils.Txt

/**
 * Quién puede ver una receta. Se guarda como texto en Room (`cocktails.visibility`) y en Firestore
 * (`users/{uid}/recipes/{id}.visibility`). Las privadas no se suben.
 */
enum class RecipeVisibility(val id: String, @StringRes val labelRes: Int, @StringRes val descriptionRes: Int) {
    PRIVATE("private", R.string.visibility_private, R.string.visibility_private_desc),
    FOLLOWERS("followers", R.string.visibility_followers, R.string.visibility_followers_desc),
    PUBLIC("public", R.string.visibility_public, R.string.visibility_public_desc);

    /** Nombre visible (sale de strings.xml). */
    val label: String get() = Txt.get(labelRes)
    val description: String get() = Txt.get(descriptionRes)

    companion object {
        fun from(id: String?): RecipeVisibility = entries.firstOrNull { it.id == id } ?: PRIVATE
    }
}
