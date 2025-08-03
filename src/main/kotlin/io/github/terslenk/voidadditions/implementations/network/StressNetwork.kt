package io.github.terslenk.voidadditions.implementations.network

import org.bukkit.block.BlockFace
import xyz.xenondevs.commons.collections.firstInstanceOfOrNull
import xyz.xenondevs.nova.world.block.tileentity.network.Network
import xyz.xenondevs.nova.world.block.tileentity.network.NetworkData
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkConnectionType

class StressNetwork(data: NetworkData<StressNetwork>) : Network<StressNetwork>, NetworkData<StressNetwork> by data {

    private var networkRPM: Float = 0.0f
    private var totalStressCapacity: Float = 0.0f
    private var totalStressConsumption: Float = 0.0f
    private var isOverstressed: Boolean = false

    fun tick() {
        // Get all stress holders in this network
        val stressHolders: List<StressHolder> = nodes.values.flatMap { connection ->
            when (val networkNode = connection.node) {
                is NetworkEndPoint -> networkNode.holders.filterIsInstance<StressHolder>()
                else -> emptyList()
            }
        }

        if (stressHolders.isEmpty()) {
            networkRPM = 0.0f
            return
        }

        // Calculate what RPM the network should run at
        calculateNetworkRPM(stressHolders)

        // Update all components to the network RPM
        stressHolders.forEach { it.rpm = networkRPM }

        // Calculate total capacity and consumption at current RPM
        val generators = stressHolders.filter { it.stressHolderType == StressHolderType.GENERATOR }
        val machines = stressHolders.filter { it.stressHolderType == StressHolderType.MACHINE }

        totalStressCapacity = generators.sumOf { it.currentStress.toDouble() }.toFloat()
        totalStressConsumption = machines.sumOf { it.currentStress.toDouble() }.toFloat()

        // Check if network is overstressed
        isOverstressed = totalStressConsumption > totalStressCapacity

        // If overstressed, shut down the network (RPM = 0)
        if (isOverstressed) {
            networkRPM = 0.0f
            stressHolders.forEach { it.rpm = 0.0f }
            totalStressCapacity = 0.0f
            totalStressConsumption = 0.0f
        }
    }

    private fun calculateNetworkRPM(stressHolders: List<StressHolder>) {
        val generators = stressHolders.filter { it.stressHolderType == StressHolderType.GENERATOR }
        if (generators.isEmpty()) {
            networkRPM = 0.0f
            return
        }

        // For now, use a base speed
        val baseRPM = 16f // Water wheel equivalent speed

        // Check if this RPM would cause overstress
        val machines = stressHolders.filter { it.stressHolderType == StressHolderType.MACHINE }
        val potentialCapacity = generators.sumOf { (it.baseSU * baseRPM).toDouble() }.toFloat()
        val potentialConsumption = machines.sumOf { (it.baseSU * baseRPM).toDouble() }.toFloat()

        networkRPM = if (potentialConsumption <= potentialCapacity) {
            baseRPM.coerceAtMost(256f) // Max RPM limit
        } else {
            0.0f // Would be overstressed
        }
    }

    /**
     * Get current network statistics
     */
    fun getNetworkStats(): StressNetworkStats {
        return StressNetworkStats(
            rpm = networkRPM,
            capacity = totalStressCapacity,
            consumption = totalStressConsumption,
            isOverstressed = isOverstressed,
            efficiency = if (totalStressCapacity > 0) (totalStressConsumption / totalStressCapacity) else 0f
        )
    }

    companion object {
        fun validateLocal(from: NetworkEndPoint, to: NetworkEndPoint, face: BlockFace): Boolean {
            val stressHolderFrom: StressHolder = from.holders.firstInstanceOfOrNull<StressHolder>() ?: return false
            val stressHolderTo: StressHolder = to.holders.firstInstanceOfOrNull<StressHolder>() ?: return false
            val conFrom: NetworkConnectionType? = stressHolderFrom.connectionConfig[face]
            val conTo: NetworkConnectionType? = stressHolderTo.connectionConfig[face.oppositeFace]

            return conFrom != conTo || conFrom == NetworkConnectionType.BUFFER
        }
    }
}