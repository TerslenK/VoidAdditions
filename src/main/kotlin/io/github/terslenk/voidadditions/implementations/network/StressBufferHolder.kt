package io.github.terslenk.voidadditions.implementations.network

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkConnectionType

/**
 * Implementation for blocks that can both generate and consume stress
 * (like gearboxes or transmission blocks)
 */
class StressBufferHolder(
    override val blockedFaces: Set<BlockFace> = emptySet(),
    override val baseSU: Float,
    override var rpm: Float
) : StressHolder {

    override val connectionConfig: MutableMap<BlockFace, NetworkConnectionType> = mutableMapOf()
    override val stressHolderType: StressHolderType = StressHolderType.MACHINE // Neutral, doesn't generate
    val stressValue: Long = 0 // No consumption or generation
    var speed: Float = 1.0f // Passes through at full speed

    init {
        // Initialize all non-blocked faces as buffers
        BlockFace.entries.forEach { face ->
            if (face !in blockedFaces) {
                connectionConfig[face] = NetworkConnectionType.BUFFER
            }
        }
    }

    fun setConnectionType(face: BlockFace, type: NetworkConnectionType) {
        if (face !in blockedFaces) {
            connectionConfig[face] = type
        }
    }
}