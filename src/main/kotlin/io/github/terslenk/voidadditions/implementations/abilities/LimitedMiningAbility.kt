package io.github.terslenk.voidadditions.implementations.abilities


import org.bukkit.entity.Player
import xyz.xenondevs.nova.world.player.ability.Ability


class LimitedMiningAbility(player: Player) : Ability(player) {
    override fun handleRemove() {
        player.sendMessage("Abilities removed from ${player.name}")
    }
    
    override fun handleTick() {

    }
}