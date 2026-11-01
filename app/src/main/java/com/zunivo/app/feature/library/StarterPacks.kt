package com.zunivo.app.feature.library

import androidx.annotation.StringRes
import com.zunivo.app.R

/** Ready-made pack ideas. Icons are fetched live from Iconify search using these English keywords. */
enum class StarterPack(@StringRes val nameRes: Int, val seeds: List<String>) {
    RESTAURANTS(R.string.pack_restaurants, listOf("restaurant", "food", "coffee")),
    CLOTHES(R.string.pack_clothes, listOf("shirt", "dress", "shoe")),
    ELECTRONICS(R.string.pack_electronics, listOf("laptop", "phone", "headphones")),
    SALONS(R.string.pack_salons, listOf("scissors", "hair", "beauty")),
    GAMES(R.string.pack_games, listOf("gamepad", "dice", "trophy")),
    HOME(R.string.pack_home, listOf("home", "sofa", "lamp")),
    DELIVERY(R.string.pack_delivery, listOf("delivery", "truck", "package")),
    EDUCATION(R.string.pack_education, listOf("school", "book", "graduation")),
    TRAVEL(R.string.pack_travel, listOf("plane", "map", "suitcase")),
    BUSINESS(R.string.pack_business, listOf("briefcase", "chart", "handshake")),
    HEALTH(R.string.pack_health, listOf("medical", "heartbeat", "hospital")),
    TECH(R.string.pack_tech, listOf("code", "cloud", "server")),
    SHOPS(R.string.pack_shops, listOf("shop", "cart", "store")),
}
