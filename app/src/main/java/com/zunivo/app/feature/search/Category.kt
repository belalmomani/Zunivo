package com.zunivo.app.feature.search

import androidx.annotation.StringRes
import com.zunivo.app.R

/** Zunivo's own category list (Iconify has no ready-made taxonomy; mapping is added in phase 3). */
enum class Category(@StringRes val labelRes: Int) {
    TECH(R.string.cat_tech),
    BUSINESS(R.string.cat_business),
    SHOPS(R.string.cat_shops),
    FOOD(R.string.cat_food),
    NATURE(R.string.cat_nature),
    HEALTH(R.string.cat_health),
    EDUCATION(R.string.cat_education),
    GAMES(R.string.cat_games),
    TRAVEL(R.string.cat_travel),
    HOME(R.string.cat_home),
    COMMUNICATION(R.string.cat_communication),
    FINANCE(R.string.cat_finance),
    TRANSPORT(R.string.cat_transport),
    SOCIAL(R.string.cat_social),
    GENERAL(R.string.cat_general),
}
