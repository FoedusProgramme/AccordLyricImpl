package org.foedusprogramme.accordlyricimpl

open class LyricBase

data class Lyric(
    val content: String
) : LyricBase()

data class Creator(
    val content: String
) : LyricBase()