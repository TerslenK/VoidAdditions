package io.github.terslenk.voidadditions.implementations.events

import io.github.terslenk.voidadditions.implementations.VoidAbilities
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import xyz.xenondevs.nova.initialize.Init
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InitStage
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.world.player.ability.AbilityManager

@Init(stage = InitStage.POST_WORLD)
object JoinEvent: Listener {
    @InitFun
    private fun init() {
        registerEvents()
    }
    
    @EventHandler
    fun joinEvent(player: PlayerJoinEvent) {
        val p = player.player
        if ((!p.hasPlayedBefore() || p.hasPermission("limitedMining" )) && !AbilityManager.hasAbility(p, VoidAbilities.LIMITED_MINING)) {
            p.sendMessage("Kırma yetkin alındı")
            AbilityManager.giveAbility(p, VoidAbilities.LIMITED_MINING)
        } else {
            p.sendMessage("Kırma yetkin alınmadı")
            
        }
    }
}