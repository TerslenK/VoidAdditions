package io.github.terslenk.voidadditions.implementations.network

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkConnectionType

/**
 * Implementation for stress-consuming machines
 */
class StressMachineHolder(
    baseStressUsage: Long,
    override val blockedFaces: Set<BlockFace> = emptySet(),
    initialSpeed: Float = 0.0f, override val baseSU: Float, override var rpm: Float
) : StressHolder {

    override val connectionConfig: MutableMap<BlockFace, NetworkConnectionType> = mutableMapOf()
    override val stressHolderType: StressHolderType = StressHolderType.MACHINE
    val stressValue: Long = baseStressUsage
    var speed: Float = initialSpeed
        set(value) {
            field = value.coerceIn(0.0f, 1.0f)
        }

    init {
        // Initialize all non-blocked faces as inserters (machines consume stress)
        BlockFace.entries.forEach { face ->
            if (face !in blockedFaces) {
                connectionConfig[face] = NetworkConnectionType.INSERT
            }
        }
    }

    fun setConnectionType(face: BlockFace, type: NetworkConnectionType) {
        if (face !in blockedFaces) {
            connectionConfig[face] = type
        }
    }

    /**
     * Check if this machine is currently running
     */
    fun isRunning(): Boolean = speed > 0.0f

    /**
     * Get the current processing rate as a percentage
     */
    fun getProcessingRate(): Float = speed
}