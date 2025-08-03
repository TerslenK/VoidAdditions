package io.github.terslenk.voidadditions.implementations.network

import xyz.xenondevs.nova.world.block.tileentity.network.NetworkGroup
import xyz.xenondevs.nova.world.block.tileentity.network.NetworkGroupData

class StressNetworkGroup (data: NetworkGroupData<StressNetwork>) : NetworkGroup<StressNetwork>, NetworkGroupData<StressNetwork> by data {

    override fun tick() {
        networks.forEach(StressNetwork::tick)
    }

}