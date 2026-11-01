package com.zunivo.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.zunivo.app.R
import com.zunivo.app.i18n.AppLanguage
import com.zunivo.app.ui.navigation.NavState
import com.zunivo.app.ui.navigation.SubScreen
import com.zunivo.app.ui.navigation.Tab
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zunivo.app.domain.CollectionKind
import com.zunivo.app.feature.editor.EditorViewModel
import com.zunivo.app.feature.library.LibraryMessage
import com.zunivo.app.feature.library.LibraryViewModel
import com.zunivo.app.feature.search.SearchViewModel
import com.zunivo.app.ui.screens.CollectionsScreen
import com.zunivo.app.ui.screens.StoreScreen
import com.zunivo.app.ui.screens.DesignersScreen
import com.zunivo.app.ui.screens.launchShare
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zunivo.app.ui.screens.AboutScreen
import com.zunivo.app.ui.screens.EditorScreen
import com.zunivo.app.ui.screens.ImageStudioScreen
import com.zunivo.app.ui.screens.ComingSoonScreen
import com.zunivo.app.ui.screens.FavoritesScreen
import com.zunivo.app.ui.screens.HelpScreen
import com.zunivo.app.ui.screens.HomeScreen
import com.zunivo.app.ui.screens.MoreScreen
import com.zunivo.app.ui.screens.PrivacyScreen
import com.zunivo.app.ui.screens.SearchScreen
import com.zunivo.app.ui.screens.SettingsScreen
import com.zunivo.app.ui.screens.SplashScreen

private val NavStateSaver = listSaver<NavState, String>(
    save = { state -> listOf(state.tab.name, state.sub?.name.orEmpty()) },
    restore = { saved ->
        NavState(
            tab = Tab.valueOf(saved[0]),
            sub = saved[1].takeIf { name -> name.isNotEmpty() }?.let { name -> SubScreen.valueOf(name) },
        )
    },
)

private data class TabItem(val tab: Tab, val labelRes: Int, val icon: ImageVector)

private val tabs = listOf(
    TabItem(Tab.HOME, R.string.nav_home, Icons.Filled.Home),
    TabItem(Tab.SEARCH, R.string.nav_search, Icons.Filled.Search),
    TabItem(Tab.FAVORITES, R.string.nav_favorites, Icons.Filled.Favorite),
    TabItem(Tab.MORE, R.string.nav_more, Icons.Filled.Menu),
)

@Composable
fun ZunivoRoot(currentLanguage: AppLanguage, onLanguageChange: (AppLanguage) -> Unit) {
    var showSplash by rememberSaveable { mutableStateOf(true) }
    if (showSplash) {
        SplashScreen(onFinished = { showSplash = false })
    } else {
        MainShell(currentLanguage, onLanguageChange)
    }
}

@Composable
private fun MainShell(currentLanguage: AppLanguage, onLanguageChange: (AppLanguage) -> Unit) {
    var nav by rememberSaveable(stateSaver = NavStateSaver) { mutableStateOf(NavState()) }
    val editorVm: EditorViewModel = viewModel()
    val libVm: LibraryViewModel = viewModel()
    val searchVm: SearchViewModel = viewModel()
    val context = LocalContext.current
    val lib by libVm.state.collectAsStateWithLifecycle()
    val libMessage = lib.message
    LaunchedEffect(libMessage, lib.args) {
        if (libMessage != null) {
            Toast.makeText(context, libraryMessageText(context, libMessage, lib.args), Toast.LENGTH_SHORT).show()
            libVm.consumeMessage()
        }
    }
    val libShare = lib.share
    LaunchedEffect(libShare) {
        if (libShare != null) {
            launchShare(context, libShare)
            libVm.consumeShare()
        }
    }
    BackHandler(enabled = nav.canGoBack()) { nav = nav.back() }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { item ->
                    NavigationBarItem(
                        selected = nav.tab == item.tab,
                        onClick = { nav = nav.selectTab(item.tab) },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(stringResource(item.labelRes)) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            val sub = nav.sub
            if (sub == null) {
                when (nav.tab) {
                    Tab.HOME -> HomeScreen(
                        onOpenSearch = { nav = nav.selectTab(Tab.SEARCH) },
                        onOpen = { screen -> nav = nav.open(screen) },
                    )
                    Tab.SEARCH -> SearchScreen(onOpenEditor = { item ->
                        editorVm.open(item)
                        nav = nav.open(SubScreen.EDITOR)
                    })
                    Tab.FAVORITES -> FavoritesScreen(onOpenIcon = { item ->
                        searchVm.select(item)
                        nav = nav.selectTab(Tab.SEARCH)
                    })
                    Tab.MORE -> MoreScreen(onOpen = { screen -> nav = nav.open(screen) })
                }
            } else {
                val goBack = { nav = nav.back() }
                when (sub) {
                    SubScreen.SETTINGS -> SettingsScreen(
                        currentLanguage = currentLanguage,
                        onLanguageChange = onLanguageChange,
                        onOpen = { screen -> nav = nav.open(screen) },
                        onBack = goBack,
                    )
                    SubScreen.EDITOR -> EditorScreen(onBack = goBack, onGoSearch = { nav = nav.selectTab(Tab.SEARCH) }, vm = editorVm)
                    SubScreen.STUDIO -> ImageStudioScreen(onBack = goBack)
                    SubScreen.FOLDERS -> CollectionsScreen(CollectionKind.FOLDER, goBack)
                    SubScreen.PACKS -> CollectionsScreen(CollectionKind.PACK, goBack)
                    SubScreen.DESIGNERS -> DesignersScreen(goBack)
                    SubScreen.STORE -> StoreScreen(goBack)
                    SubScreen.ABOUT -> AboutScreen(goBack)
                    SubScreen.PRIVACY -> PrivacyScreen(goBack)
                    SubScreen.HELP -> HelpScreen(goBack)
                    else -> ComingSoonScreen(sub, goBack)
                }
            }
        }
    }
}

private fun libraryMessageText(context: android.content.Context, m: LibraryMessage, args: List<Int>): String = when (m) {
    LibraryMessage.CREATED -> context.getString(R.string.msg_created)
    LibraryMessage.RENAMED -> context.getString(R.string.msg_renamed)
    LibraryMessage.DUPLICATED -> context.getString(R.string.msg_duplicated)
    LibraryMessage.DELETED -> context.getString(R.string.msg_deleted)
    LibraryMessage.ADDED -> context.getString(R.string.msg_added)
    LibraryMessage.ALREADY_THERE -> context.getString(R.string.msg_already_there)
    LibraryMessage.FULL -> context.getString(R.string.msg_collection_full, args.firstOrNull() ?: 0)
    LibraryMessage.FAVORITE_ADDED -> context.getString(R.string.msg_favorite_added)
    LibraryMessage.FAVORITE_REMOVED -> context.getString(R.string.msg_favorite_removed)
    LibraryMessage.EXPORTED -> context.getString(R.string.msg_pack_exported, args.getOrElse(0) { 0 }, args.getOrElse(1) { 0 })
    LibraryMessage.EXPORT_EMPTY -> context.getString(R.string.msg_export_empty)
    LibraryMessage.EXPORT_FAILED -> context.getString(R.string.msg_export_failed)
    LibraryMessage.OFFLINE -> context.getString(R.string.msg_offline)
    LibraryMessage.STARTER_FAILED -> context.getString(R.string.msg_starter_failed)
}
