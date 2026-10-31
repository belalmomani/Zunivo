package com.zunivo.app.ui.navigation

enum class Tab { HOME, SEARCH, FAVORITES, MORE }

enum class SubScreen { FOLDERS, EDITOR, STUDIO, PACKS, DESIGNERS, STORE, DOWNLOADS, SETTINGS, HELP, PRIVACY, ABOUT }

data class NavState(val tab: Tab = Tab.HOME, val sub: SubScreen? = null) {
    fun selectTab(newTab: Tab): NavState = NavState(newTab, null)
    fun open(screen: SubScreen): NavState = copy(sub = screen)
    fun canGoBack(): Boolean = sub != null || tab != Tab.HOME
    fun back(): NavState = when {
        sub != null -> copy(sub = null)
        tab != Tab.HOME -> NavState()
        else -> this
    }
}
