package io.github.terslenk.voidadditions.implementations.events

import io.github.terslenk.voidadditions.implementations.VoidAbilities
import io.github.terslenk.voidadditions.implementations.utils.VoidUtils
import org.bukkit.GameMode
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockDamageEvent
import org.bukkit.event.player.PlayerJoinEvent
import xyz.xenondevs.nova.config.Configs
import xyz.xenondevs.nova.initialize.Init
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InitStage
import xyz.xenondevs.nova.util.item.ItemUtils
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.world.player.ability.AbilityManager

@Init(stage = InitStage.POST_WORLD)
object VoidEvents: Listener {
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
    
    @EventHandler
    fun blockDamageEvent(event: BlockDamageEvent) {
        val player = event.player
        val blockType = event.block.type.name.uppercase()
        val itemId = ItemUtils.getId(player.inventory.itemInMainHand)
        
        // Load whitelisted blocks and tools from the configuration
        val config = Configs["void_additions:main_config"]
        val whitelistedBlocks = VoidUtils.loadConfigAsList(config, "default_whitelisted_blocks")
        val whitelistedTools = VoidUtils.loadConfigAsList(config, "whitelisted_tools")
        
        // Check conditions for cancelling the block damage event
        if (player.gameMode != GameMode.SURVIVAL || !AbilityManager.hasAbility(player, VoidAbilities.LIMITED_MINING)) return
        
        if (!whitelistedTools.contains(itemId.uppercase()) and !whitelistedBlocks.contains(blockType)) {
            event.isCancelled = true
            player.sendMessage("[DEBUG] You can't mine $blockType with $itemId")
        }
    }
    
}