package com.paba.notes.helper

fun generateToken(): String {
    val characters = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    return String(CharArray(16) { characters.random() })
}