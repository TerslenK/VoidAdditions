package io.github.terslenk.voidadditions.implementations.network

import org.bukkit.block.BlockFace
import xyz.xenondevs.commons.collections.enumSet
import xyz.xenondevs.nova.world.block.tileentity.network.node.EndPointDataHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkConnectionType

interface StressHolder : EndPointDataHolder {
    /**
     * The [BlockFaces][BlockFace] that can never have a connection.
     */
    val blockedFaces: Set<BlockFace>

    /**
     * Stores which [NetworkConnectionType] is used for each [BlockFace].
     */
    val connectionConfig: MutableMap<BlockFace, NetworkConnectionType>

    /**
     * The type of stress holder this is
     */
    val stressHolderType: StressHolderType

    /**
     * Base stress value (SU per RPM)
     * For generators: how much stress capacity per RPM
     * For machines: how much stress consumption per RPM
     */
    val baseSU: Float

    /**
     * Current RPM of this component
     * All components in a network should have the same RPM
     */
    var rpm: Float

    /**
     * Current stress capacity (for generators) or consumption (for machines)
     * Calculated as baseSU × rpm
     */
    val currentStress: Float
        get() = baseSU * rpm

    /**
     * Maximum RPM this component can handle before breaking
     * Default is 256 RPM like in Create mod
     */
    val maxRPM: Float
        get() = 256f

    override val allowedFaces: Set<BlockFace>
        get() = connectionConfig.mapNotNullTo(enumSet()) { (face, type) ->
            if (type != NetworkConnectionType.NONE) face else null
        }
}