package io

interface TextHandler {
    fun handleChar(c: Char)
    fun handleSpecialKey(s: SpecialChar)
}