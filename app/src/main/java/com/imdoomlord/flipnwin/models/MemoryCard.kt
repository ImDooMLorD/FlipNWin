package com.imdoomlord.flipnwin.models

data class MemoryCard(
    // data class of one MemoryCard.
    val identifier: Int,
    val imageUrl: String? = null,           // Default to null when normal games are played
    var isFaceUp: Boolean = false,
    var isMatched: Boolean = false
)
