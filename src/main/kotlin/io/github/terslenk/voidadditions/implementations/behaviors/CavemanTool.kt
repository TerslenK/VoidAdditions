package io.github.terslenk.voidadditions.implementations.behaviors

import io.github.terslenk.voidadditions.implementations.registry.VoidUtils
import org.bukkit.attribute.Attribute
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.inventory.ItemStack
import org.spongepowered.configurate.ConfigurationNode
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.world.item.NovaItem
import xyz.xenondevs.nova.world.item.behavior.ItemBehavior
import xyz.xenondevs.nova.world.item.behavior.ItemBehaviorFactory
import xyz.xenondevs.nova.world.player.WrappedPlayerInteractEvent
import java.util.UUID
import kotlin.math.round
import kotlin.uuid.Uuid

class CavemanTool : ItemBehavior {
    
    private var whitelistedBlocks: Map<String, Map<String, String>> = emptyMap()
    
    companion object : ItemBehaviorFactory<CavemanTool> {
        override fun create(item: NovaItem): CavemanTool {
            val cavemanTool = CavemanTool()
            cavemanTool.loadConfig(item.config)
            return cavemanTool
        }
    }
    
    private fun loadConfig(cfg: Provider<ConfigurationNode>) {
        whitelistedBlocks = VoidUtils.loadConfigAsMap(cfg, "whitelisted_blocks")
    }
    
    /**
     * Handles player interaction events. Placeholder for future functionality.
     */
    override fun handleInteract(
        player: Player,
        itemStack: ItemStack,
        action: Action,
        wrappedEvent: WrappedPlayerInteractEvent
    ) {}
    
    /**
     * Handles block breaking events. Replaces blocks based on the configuration.
     */
    override fun handleBreakBlock(player: Player, itemStack: ItemStack, event: BlockBreakEvent) {
        val blockType = event.block.type.name.uppercase()
        val properties = whitelistedBlocks[blockType]
        
        if (properties != null) {
            val replaceWith = properties["replace_with"]
            val dropItem = properties["drop_item"]
            val chance: Int = properties["chance"]?.toInt() ?: 100
            if (replaceWith != null && dropItem != null) {
                VoidUtils.handleBlockReplacement(event, replaceWith, dropItem, chance)
            }
        }
        itemStack.damage(1, player)
    }
    
    override fun modifyBlockDamage(player: Player, itemStack: ItemStack, block: Block, damage: Double): Double {
        val target = player.getTargetBlock(null, round(player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE)?.value ?: 7.0).toInt()).type.name.uppercase()
        player.sendMessage(whitelistedBlocks.toString())

        return if (whitelistedBlocks[target] != null) {
            when (whitelistedBlocks[target]?.get("break_speed")) {
                "default" -> {
                    damage
                } else -> {
                0.05
            }
            }
        } else {
            -1.0
        }
    }
}