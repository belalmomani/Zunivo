package com.zunivo.app.domain

/** Relative brightness 0..1 of a #RRGGBB colour (used to pick a readable preview background). */
fun HexColor.luminance(hex: String?): Double {
    val n = normalize(hex) ?: return 0.0
    val r = n.substring(1, 3).toInt(16)
    val g = n.substring(3, 5).toInt(16)
    val b = n.substring(5, 7).toInt(16)
    return (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
}
