package io.github.terslenk.voidadditions.implementations.behaviors

import io.github.terslenk.voidadditions.implementations.VoidItems
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.util.damageItemInMainHand
import xyz.xenondevs.nova.util.playSoundNearby
import xyz.xenondevs.nova.world.item.NovaItem
import xyz.xenondevs.nova.world.item.behavior.ItemBehavior
import xyz.xenondevs.nova.world.item.behavior.ItemBehaviorFactory
import kotlin.math.round
import kotlin.random.Random

class CavemanTool(
    breakSpeed: Provider<Double>,
) : ItemBehavior {
    private val breakSpeed by breakSpeed
    
    companion object : ItemBehaviorFactory<CavemanTool> {
        override fun create(item: NovaItem): CavemanTool {
            val cfg = item.config
            return CavemanTool(
                cfg.entry<Double>("break_speed"),
            )
        }
    }
    
    override fun handleBreakBlock(player: Player, itemStack: ItemStack, event: BlockBreakEvent) {
        when (event.block.type) {
            Material.STONE -> {
                val amount = Random.nextInt(1,2)
                player.damageItemInMainHand(1)
                event.block.type = Material.COBBLESTONE
                event.block.location.playSoundNearby(Sound.BLOCK_STONE_BREAK,1f,1f)
                player.world.dropItem(event.block.location, VoidItems.STONE_PEBBLE.createItemStack(amount))
                player.sendMessage("Dropped $amount of pebble")
                event.isCancelled = true
            }
            Material.COBBLESTONE -> {
                val amount = Random.nextInt(1,2)
                player.damageItemInMainHand(1)
                event.block.type = Material.GRAVEL
                event.block.location.playSoundNearby(Sound.BLOCK_STONE_BREAK,1f,1f)
                player.world.dropItem(event.block.location, VoidItems.STONE_PEBBLE.createItemStack(amount))
                player.sendMessage("Dropped $amount of pebble")
                event.isCancelled = true
            }
            Material.OAK_LOG -> {
                player.damageItemInMainHand(1)
                event.block.type = Material.STRIPPED_OAK_LOG
                event.block.location.playSoundNearby(Sound.ITEM_AXE_STRIP,1f,1f)
                player.world.dropItem(event.block.location, VoidItems.OAK_BARK.createItemStack(1))
                event.isCancelled = true
            }
            Material.SPRUCE_LOG -> {
                player.damageItemInMainHand(1)
                event.block.type = Material.STRIPPED_SPRUCE_LOG
                event.block.location.playSoundNearby(Sound.ITEM_AXE_STRIP,1f,1f)
                player.world.dropItem(event.block.location, VoidItems.SPRUCE_BARK.createItemStack(1))
                event.isCancelled = true
            }
            Material.BIRCH_LOG -> {
                player.damageItemInMainHand(1)
                event.block.type = Material.STRIPPED_BIRCH_LOG
                event.block.location.playSoundNearby(Sound.ITEM_AXE_STRIP,1f,1f)
                player.world.dropItem(event.block.location, VoidItems.BIRCH_BARK.createItemStack(1))
                event.isCancelled = true
            }
            Material.JUNGLE_LOG -> {
                player.damageItemInMainHand(1)
                event.block.type = Material.STRIPPED_JUNGLE_LOG
                event.block.location.playSoundNearby(Sound.ITEM_AXE_STRIP,1f,1f)
                player.world.dropItem(event.block.location, VoidItems.JUNGLE_BARK.createItemStack(1))
                event.isCancelled = true
            }
            Material.ACACIA_LOG -> {
                player.damageItemInMainHand(1)
                event.block.type = Material.STRIPPED_ACACIA_LOG
                event.block.location.playSoundNearby(Sound.ITEM_AXE_STRIP,1f,1f)
                player.world.dropItem(event.block.location, VoidItems.ACACIA_BARK.createItemStack(1))
                event.isCancelled = true
            }
            Material.DARK_OAK_LOG -> {
                player.damageItemInMainHand(1)
                event.block.type = Material.STRIPPED_DARK_OAK_LOG
                event.block.location.playSoundNearby(Sound.ITEM_AXE_STRIP,1f,1f)
                player.world.dropItem(event.block.location, VoidItems.DARK_OAK_BARK.createItemStack(1))
                event.isCancelled = true
            }
            Material.MANGROVE_LOG -> {
                player.damageItemInMainHand(1)
                event.block.type = Material.STRIPPED_MANGROVE_LOG
                event.block.location.playSoundNearby(Sound.ITEM_AXE_STRIP,1f,1f)
                player.world.dropItem(event.block.location, VoidItems.MANGROVE_BARK.createItemStack(1))
                event.isCancelled = true
            }
            Material.CHERRY_LOG -> {
                player.damageItemInMainHand(1)
                event.block.type = Material.STRIPPED_OAK_LOG
                event.block.location.playSoundNearby(Sound.ITEM_AXE_STRIP,1f,1f)
                player.world.dropItem(event.block.location, VoidItems.CHERRY_BARK.createItemStack(1))
                event.isCancelled = true
            }
            Material.GRAVEL -> {
                event.isCancelled = false
            }
            else -> {
                event.isCancelled = true
            }
        }
    }
    
    override fun modifyBlockDamage(player: Player, itemStack: ItemStack, damage: Double): Double {
        return when (player.getTargetBlock(null,round(player.getAttribute(Attribute.PLAYER_BLOCK_INTERACTION_RANGE)?.value ?: 7.0).toInt()).type) {
            Material.STONE -> breakSpeed / 2
            Material.COBBLESTONE -> breakSpeed
            Material.GRAVEL -> breakSpeed * 2
            
            Material.OAK_LOG -> breakSpeed * 1.2
            Material.SPRUCE_LOG -> breakSpeed * 1.2
            Material.BIRCH_LOG -> breakSpeed * 1.2
            Material.JUNGLE_LOG -> breakSpeed * 1.2
            Material.ACACIA_LOG -> breakSpeed * 1.2
            Material.DARK_OAK_LOG -> breakSpeed * 1.2
            Material.MANGROVE_LOG -> breakSpeed * 1.2
            Material.CHERRY_LOG -> breakSpeed * 1.2
            
            else -> -1.0
        }
    }
}