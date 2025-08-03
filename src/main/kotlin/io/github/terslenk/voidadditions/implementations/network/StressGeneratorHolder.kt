package io.github.terslenk.voidadditions.implementations.network

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkConnectionType

/**
 * Implementation for stress generators (like waterwheels)
 */
class StressGeneratorHolder(
    stressCapacity: Long,
    override val blockedFaces: Set<BlockFace> = emptySet(),
    override val baseSU: Float,
    override var rpm: Float
) : StressHolder {

    override val connectionConfig: MutableMap<BlockFace, NetworkConnectionType> = mutableMapOf()
    override val stressHolderType: StressHolderType = StressHolderType.GENERATOR
    val stressValue: Long = stressCapacity
    var speed: Float = 1.0f // Generators always run at full speed

    init {
        // Initialize all non-blocked faces as extractors (generators provide stress)
        BlockFace.entries.forEach { face ->
            if (face !in blockedFaces) {
                connectionConfig[face] = NetworkConnectionType.EXTRACT
            }
        }
    }

    fun setConnectionType(face: BlockFace, type: NetworkConnectionType) {
        if (face !in blockedFaces) {
            connectionConfig[face] = type
        }
    }
}