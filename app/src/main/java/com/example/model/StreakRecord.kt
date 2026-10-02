package com.example.model

data class StreakRecord(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val multiplier: Double,
    val isUnderThreshold: Boolean,
    val streakAfterRound: Int,
    val rawText: String = ""
)
