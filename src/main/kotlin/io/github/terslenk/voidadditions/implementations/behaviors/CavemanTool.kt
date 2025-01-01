package io.github.terslenk.voidadditions.implementations.behaviors

import io.github.terslenk.voidadditions.implementations.utils.VoidUtils
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.inventory.ItemStack
import org.spongepowered.configurate.ConfigurationNode
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.util.dropItem
import xyz.xenondevs.nova.util.item.ItemUtils
import xyz.xenondevs.nova.world.item.NovaItem
import xyz.xenondevs.nova.world.item.behavior.ItemBehavior
import xyz.xenondevs.nova.world.item.behavior.ItemBehaviorFactory
import xyz.xenondevs.nova.world.player.WrappedPlayerInteractEvent
import kotlin.math.round

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
    ) {
        // Placeholder for future interactions
        if (action.isRightClick) {
            println("Loaded Config: $whitelistedBlocks")
        }
    }
    
    /**
     * Handles block breaking events. Replaces blocks based on the configuration.
     */
    override fun handleBreakBlock(player: Player, itemStack: ItemStack, event: BlockBreakEvent) {
        val blockType = event.block.type.name.uppercase()
        val properties = whitelistedBlocks[blockType]
        
        if (properties != null) {
            val replaceWith = properties["replace_with"]
            val dropItem = properties["drop_item"]
            if (replaceWith != null && dropItem != null) {
                handleBlockReplacement(event, blockType, replaceWith, dropItem)
            }
        }
        itemStack.damage(1, player)
    }
    
    override fun modifyBlockDamage(player: Player, itemStack: ItemStack, block: Block, damage: Double): Double {
        val target = player.getTargetBlock(null, round(player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE)?.value ?: 7.0).toInt()).type.name.uppercase()
        
        return if (whitelistedBlocks.contains(target)) {
            whitelistedBlocks[target]?.get("break_speed")?.toDouble()?.div(25) ?: damage
        } else {
            -1.0
        }
    }
    
    /**
     * Handles the replacement logic for blocks.
     */
    private fun handleBlockReplacement(event: BlockBreakEvent, blockType: String, replaceWith: String, dropItem: String) {
        val player = event.player // Get the player who broke the block
        val location = event.block.location // Get the block's location
        
        when (replaceWith.uppercase()) {
            "NOTHING" -> {
                println("Block $blockType will drop naturally.")
                // Play the breaking sound of the original block
                player.playSound(location, event.block.blockData.soundGroup.breakSound, 1.0f, 1.0f)
            }
            else -> {
                val newBlockType = Material.matchMaterial(replaceWith)
                if (newBlockType != null) {
                    event.isCancelled = true
                    event.block.type = newBlockType
                    event.block.location.dropItem(ItemUtils.getItemStack(dropItem))
                    
                    // Play the breaking sound of the original block
                    player.playSound(location, event.block.blockData.soundGroup.breakSound, 1.0f, 1.0f)
                    println("Replaced $blockType with $newBlockType")
                } else {
                    println("Invalid replacement block type: $replaceWith")
                }
            }
        }
    }
}