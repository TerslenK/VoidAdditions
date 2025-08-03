package io.github.terslenk.voidadditions.implementations.network

enum class StressHolderType {
    GENERATOR,  // Provides stress capacity (base SU × RPM)
    MACHINE     // Consumes stress (base SU × RPM)
}