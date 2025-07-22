package ru.duester.procal

data class Program(
    val id: Long,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)
