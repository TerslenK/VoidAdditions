package io.github.terslenk.voidadditions.implementations.behaviors

import io.github.terslenk.voidadditions.implementations.registry.VoidUtils
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.Consumable
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation
import io.papermc.paper.registry.keys.SoundEventKeys
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.AbstractArrow
import org.bukkit.entity.Arrow
import org.bukkit.entity.Player
import org.bukkit.entity.SpectralArrow
import org.bukkit.event.block.Action
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta
import xyz.xenondevs.nova.network.event.serverbound.ServerboundPlayerActionPacketEvent
import xyz.xenondevs.nova.util.item.damage
import xyz.xenondevs.nova.util.playSoundNearby
import xyz.xenondevs.nova.util.runTask
import xyz.xenondevs.nova.world.item.NovaItem
import xyz.xenondevs.nova.world.item.behavior.ItemBehavior
import xyz.xenondevs.nova.world.item.behavior.ItemBehaviorFactory
import xyz.xenondevs.nova.world.player.WrappedPlayerInteractEvent
import java.util.UUID

class BowBehavior : ItemBehavior {

    private val playerTimers = mutableMapOf<UUID, Long>()

    companion object : ItemBehaviorFactory<BowBehavior> {
        override fun create(item: NovaItem): BowBehavior {
            return BowBehavior()
        }
    }

    override fun handleInteract(
        player: Player,
        itemStack: ItemStack,
        action: Action,
        wrappedEvent: WrappedPlayerInteractEvent
    ) {
        if (action.isRightClick) {
            if (VoidUtils.getArrow(player,"BOW") == null) {
                itemStack.unsetData(DataComponentTypes.CONSUMABLE)
                player.updateInventory()
            } else {
                itemStack.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable()
                    .animation(ItemUseAnimation.BOW)
                    .sound(SoundEventKeys.INTENTIONALLY_EMPTY)
                    .consumeSeconds(Float.MAX_VALUE)
                    .hasConsumeParticles(false)
                    .build())
                player.updateInventory()
            }
            playerTimers[player.uniqueId] = System.currentTimeMillis()
        }
    }

    override fun handleRelease(player: Player, itemStack: ItemStack, event: ServerboundPlayerActionPacketEvent) {
        if (event.action != ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM) return

        val startTime = playerTimers[player.uniqueId] ?: return
        val currentTime = System.currentTimeMillis()
        val multiplier = ((currentTime - startTime) / 500.0).coerceAtMost(4.0)
        val isCreative = player.gameMode == GameMode.CREATIVE
        val arrowItem = VoidUtils.getArrow(player,"BOW")

        // Check if we should shoot at all
        if (multiplier <= 0.5) return
        if (arrowItem == null && !isCreative) return

        // Determine if we should consume the arrow
        val shouldConsume = player.gameMode == GameMode.SURVIVAL && !(itemStack.enchantments.contains(Enchantment.INFINITY) && VoidUtils.getArrow(player,"BOW")?.type == Material.ARROW)
        VoidUtils.removeArrow(player, itemStack,"BOW",shouldConsume)

        runTask {
            val arrow: AbstractArrow = when (arrowItem?.type) {
                Material.SPECTRAL_ARROW -> {
                    player.launchProjectile(SpectralArrow::class.java)
                }
                Material.ARROW -> if (shouldConsume) {
                    player.launchProjectile(Arrow::class.java).apply {
                        pickupStatus = AbstractArrow.PickupStatus.ALLOWED
                    }
                } else {
                    player.launchProjectile(Arrow::class.java).apply {
                        pickupStatus = AbstractArrow.PickupStatus.CREATIVE_ONLY
                    }
                }
                Material.TIPPED_ARROW -> {
                    // Create the arrow item as a proper tipped arrow first
                    val tippedArrowItem = ItemStack(Material.TIPPED_ARROW)
                    val arrowMeta = arrowItem.itemMeta as? PotionMeta
                    tippedArrowItem.itemMeta = arrowMeta

                    // Launch with the tipped arrow item
                    val tipped = player.launchProjectile(Arrow::class.java)

                    // Some servers have this method
                    try {
                        tipped.itemStack = tippedArrowItem
                    } catch (e: Exception) {
                        // Fallback to manual effect copying
                        arrowMeta?.let { meta ->
                            meta.customEffects.forEach { effect ->
                                tipped.addCustomEffect(effect, true)
                            }
                            meta.basePotionType?.let { tipped.basePotionType = it }
                        }
                    }
                    tipped
                }
                else -> player.launchProjectile(Arrow::class.java)
            }

            arrow.velocity = player.location.direction.multiply(multiplier)
            arrow.shooter = player
            player.location.playSoundNearby(Sound.ENTITY_ARROW_SHOOT, 1f, 1f)
            if (player.gameMode != GameMode.CREATIVE) {
                itemStack.damage(1,player.world)
            }
        }

        playerTimers.remove(player.uniqueId)
    }

    override fun modifyClientSideStack(player: Player?, server: ItemStack, client: ItemStack): ItemStack {
        return client.withType(Material.BOW)
    }
}