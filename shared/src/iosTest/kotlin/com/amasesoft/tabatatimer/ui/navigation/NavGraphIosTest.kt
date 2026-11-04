package com.amasesoft.tabatatimer.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavGraphNavigator
import androidx.navigation.NavigatorProvider
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * iOS only: there a route argument without a declared NavType throws while the graph is built,
 * i.e. the app crashes at launch. Android resolves enums through JVM reflection and never fails.
 */
class NavGraphIosTest {

    /** Same declarations as [AppNavHost] for the routes with arguments. */
    @Test
    fun graphWithArgumentRoutesBuilds() {
        val provider = NavigatorProvider().apply {
            addNavigator(ComposeNavigator())
            addNavigator(NavGraphNavigator(this))
        }
        val graph = NavGraphBuilder(provider, HomeRoute, null as KClass<*>?, emptyMap()).apply {
            composable<HomeRoute> {}
            composable<EditorRoute>(typeMap = EditorRouteTypeMap) {}
            composable<RunRoute> {}
        }.build()
        assertNotNull(graph.findNode<EditorRoute>())
    }
}
