package com.amasesoft.tabatatimer.ui.navigation

import androidx.navigation.NavType
import androidx.savedstate.SavedState

/**
 * [NavType] for an enum route argument, stored by name.
 *
 * Needed for every enum in a type-safe route: Android finds enum types through JVM reflection,
 * but on iOS a missing entry in `typeMap`, or an enum not annotated with `@Serializable`, crashes
 * while the navigation graph is built.
 */
inline fun <reified E : Enum<E>> enumNavType(): NavType<E> = object : NavType<E>(isNullableAllowed = false) {
    override fun put(bundle: SavedState, key: String, value: E) = StringType.put(bundle, key, value.name)

    override fun get(bundle: SavedState, key: String): E? = StringType.get(bundle, key)?.let { enumValueOf<E>(it) }

    override fun parseValue(value: String): E = enumValueOf(value)

    override fun serializeAsValue(value: E): String = value.name
}
