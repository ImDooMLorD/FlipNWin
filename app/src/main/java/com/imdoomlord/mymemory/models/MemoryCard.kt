package com.imdoomlord.mymemory.models

data class MemoryCard(
    // data class of one MemoryCard.
    val identifier: Int,
    var isFaceUp: Boolean = false,
    var isMatched: Boolean = false
)
