package com.zunivo.app.feature.search

/** Zunivo's own category taxonomy: each category searches a few English seed keywords (approximate). */
object CategorySeeds {
    fun seeds(category: Category): List<String> = when (category) {
        Category.TECH -> listOf("computer", "code", "cloud")
        Category.BUSINESS -> listOf("briefcase", "chart", "office")
        Category.SHOPS -> listOf("shop", "cart", "store")
        Category.FOOD -> listOf("food", "restaurant", "coffee")
        Category.NATURE -> listOf("tree", "leaf", "flower")
        Category.HEALTH -> listOf("health", "medical", "heart")
        Category.EDUCATION -> listOf("book", "school", "graduation")
        Category.GAMES -> listOf("game", "gamepad", "dice")
        Category.TRAVEL -> listOf("travel", "plane", "map")
        Category.HOME -> listOf("home", "sofa", "furniture")
        Category.COMMUNICATION -> listOf("chat", "phone", "mail")
        Category.FINANCE -> listOf("money", "wallet", "bank")
        Category.TRANSPORT -> listOf("car", "bus", "train")
        Category.SOCIAL -> listOf("social", "share", "like")
        Category.GENERAL -> listOf("star", "check", "arrow")
    }
}
