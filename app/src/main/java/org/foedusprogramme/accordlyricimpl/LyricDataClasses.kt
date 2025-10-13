package org.foedusprogramme.accordlyricimpl

open class LyricBase

data class Lyric(
    val content: String,
    val isMagicLyric: Boolean = false
) : LyricBase()

data class SyncedLyric(
    val list: List<Lyric>
) : LyricBase()

data class Creator(
    val content: String
) : LyricBase()