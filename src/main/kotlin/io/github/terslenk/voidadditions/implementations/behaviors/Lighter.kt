package io.github.terslenk.voidadditions.implementations.behaviors

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.block.data.Lightable
import org.bukkit.block.data.type.Fire
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.inventory.ItemStack
import org.bukkit.util.RayTraceResult
import xyz.xenondevs.nova.util.item.damage
import xyz.xenondevs.nova.world.item.NovaItem
import xyz.xenondevs.nova.world.item.behavior.ItemBehavior
import xyz.xenondevs.nova.world.item.behavior.ItemBehaviorFactory
import xyz.xenondevs.nova.world.player.WrappedPlayerInteractEvent
import java.util.concurrent.ThreadLocalRandom


class Lighter : ItemBehavior {
    companion object : ItemBehaviorFactory<Lighter> {
        override fun create(item: NovaItem): Lighter {
            val lighter = Lighter()
            return lighter
        }
    }
    
    override fun handleInteract(
        player: Player,
        itemStack: ItemStack,
        action: Action,
        wrappedEvent: WrappedPlayerInteractEvent
    ) {
        if (action.isRightClick) {
            val maxRange = player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE)?.value?.toInt() ?: 7
            
            // Perform a ray trace to get the block face the player is looking at
            val rayTraceResult: RayTraceResult? = player.world.rayTraceBlocks(
                player.eyeLocation,
                player.location.direction,
                maxRange.toDouble()
            )
            
            if (rayTraceResult != null) {
                val hitBlock = rayTraceResult.hitBlock
                val hitBlockFace = rayTraceResult.hitBlockFace
                
                if (hitBlock != null) {
                    val blockData = hitBlock.blockData
                    val randomChance = ThreadLocalRandom.current().nextDouble()
                    
                    if (randomChance < 0.5) { // 50% chance to ignite
                        if (blockData is Lightable) {
                            if (!blockData.isLit) {
                                blockData.isLit = true
                                hitBlock.blockData = blockData // Update the block state
                                itemStack.damage(1, hitBlock.world)
                            } else {
                                player.sendMessage("The block is already lit.")
                            }
                        } else if (hitBlock.isBurnable) {
                            val fireBlockData = Bukkit.createBlockData(Material.FIRE) { bd ->
                                (bd as Fire).setFace(hitBlockFace!!.oppositeFace, true)
                            }
                            hitBlock.getRelative(hitBlockFace!!).blockData = fireBlockData
                            itemStack.damage(1, hitBlock.world)
                        } else if (hitBlockFace != null) {
                            // Attempt to set fire to the adjacent block
                            val adjacentBlock = hitBlock.getRelative(hitBlockFace)
                            
                            if (adjacentBlock.type == Material.AIR &&
                                adjacentBlock.location.clone().subtract(0.0, 1.0, 0.0).block.isSolid
                            ) {
                                adjacentBlock.type = Material.FIRE
                                itemStack.damage(1, hitBlock.world)
                            }
                        }
                    } else {
                        itemStack.damage(1, hitBlock.world)
                        player.sendMessage("No fire ignited this time!")
                    }
                }
            }
        }
    }
    
}