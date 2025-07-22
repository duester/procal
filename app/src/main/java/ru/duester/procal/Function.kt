package ru.duester.procal

data class Function(
    val id: Long,
    val programId: Long,
    val name: String,
    val isMain: Boolean = false
)