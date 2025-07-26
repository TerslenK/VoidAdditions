package io.github.terslenk.voidadditions.implementations.events

import io.github.terslenk.voidadditions.VoidAdditions
import io.github.terslenk.voidadditions.implementations.VoidAbilities
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockDamageEvent
import org.bukkit.event.player.PlayerJoinEvent
import xyz.xenondevs.nova.config.Configs
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.initialize.Init
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InitStage
import xyz.xenondevs.nova.util.item.ItemUtils
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.world.player.ability.AbilityManager

@Init(stage = InitStage.POST_WORLD)
object VoidEvents: Listener {
    val whitelistedBlocks = Configs[VoidAdditions,"limited_mining"].entry<MutableSet<String>>("default_blocks").get()
    val whitelistedTools = Configs[VoidAdditions,"limited_mining"].entry<MutableSet<String>>("overriding_tools").get()


    @InitFun
    private fun init() {
        registerEvents()
    }
    
    @EventHandler
    fun joinEvent(player: PlayerJoinEvent) {
        val p = player.player
        if ((!p.hasPlayedBefore() || p.hasPermission("limitedMining")) && !AbilityManager.hasAbility(p, VoidAbilities.LIMITED_MINING)) {
            p.sendMessage("Kırma yetkin alındı")
            AbilityManager.giveAbility(p, VoidAbilities.LIMITED_MINING)
        } else {
            p.sendMessage("Kırma yetkin alınmadı")
        }
    }

    @EventHandler
    fun blockDamageEvent(event: BlockDamageEvent) {
        val player = event.player
        val blockType = event.block.type.name.uppercase()
        val itemId = ItemUtils.getId(player.inventory.itemInMainHand).uppercase()


        if (whitelistedBlocks.contains(blockType) || whitelistedTools.contains(itemId)) {
            event.isCancelled = false
        } else {
            event.isCancelled = true
            player.sendActionBar(
                Component.text("You cant mine ").color(NamedTextColor.RED)
                    .append(Component.text(blockType).color(NamedTextColor.WHITE))
                    .append(Component.text(" using ").color(NamedTextColor.RED))
                    .append(Component.text(itemId).color(NamedTextColor.WHITE))
            )
        }
    }
}