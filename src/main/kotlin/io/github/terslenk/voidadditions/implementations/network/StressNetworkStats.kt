package io.github.terslenk.voidadditions.implementations.network

data class StressNetworkStats(
    val rpm: Float,
    val capacity: Float,
    val consumption: Float,
    val isOverstressed: Boolean,
    val efficiency: Float
)