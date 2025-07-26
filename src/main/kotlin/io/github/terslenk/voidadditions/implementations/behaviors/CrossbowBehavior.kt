package io.github.terslenk.voidadditions.implementations.behaviors

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.Consumable
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent
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
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.inventory.meta.CrossbowMeta
import xyz.xenondevs.nova.network.event.serverbound.ServerboundPlayerActionPacketEvent
import xyz.xenondevs.nova.util.playSoundNearby
import xyz.xenondevs.nova.util.runTask
import xyz.xenondevs.nova.util.runTaskTimer
import xyz.xenondevs.nova.world.item.NovaItem
import xyz.xenondevs.nova.world.item.behavior.ItemBehavior
import xyz.xenondevs.nova.world.item.behavior.ItemBehaviorFactory
import xyz.xenondevs.nova.world.player.WrappedPlayerInteractEvent
import java.util.*

class CrossbowBehavior : ItemBehavior {

    private val playerTimers = mutableMapOf<UUID, Long>()
    private val chargedCrossbows = mutableMapOf<UUID, ItemStack>() // Store charged arrow info

    companion object : ItemBehaviorFactory<CrossbowBehavior> {
        private const val CHARGE_TIME_MS = 1250L // 1.25 seconds like vanilla

        override fun create(item: NovaItem): CrossbowBehavior {
            return CrossbowBehavior()
        }
    }

    override fun handleInteract(
        player: Player,
        itemStack: ItemStack,
        action: Action,
        wrappedEvent: WrappedPlayerInteractEvent
    ) {
        if (action.isRightClick) {
            val isCharged = isChargedCrossbow(itemStack)

            if (isCharged) {
                // Fire the crossbow
                fireCrossbow(player, itemStack)
            } else {
                // Start charging
                val arrowItem = getArrow(player)
                val isCreative = player.gameMode == GameMode.CREATIVE

                if (arrowItem != null || isCreative) {
                    startCharging(player, itemStack)
                }
            }
        }
    }

    override fun handleEquip(
        player: Player,
        itemStack: ItemStack,
        slot: EquipmentSlot,
        equipped: Boolean,
        event: EntityEquipmentChangedEvent
    ) {
        val isCharged = isChargedCrossbow(itemStack)
        val hasArrow = getArrow(player) != null

        if (!isCharged && !hasArrow) {
            player.inventory.getItem(slot).unsetData(DataComponentTypes.CONSUMABLE)
        } else if (!isCharged) {
            // Can charge
            player.inventory.getItem(slot).setData(DataComponentTypes.CONSUMABLE, Consumable.consumable()
                .animation(ItemUseAnimation.CROSSBOW)
                .sound(SoundEventKeys.INTENTIONALLY_EMPTY)
                .consumeSeconds(Float.MAX_VALUE)
                .hasConsumeParticles(false)
                .build())
        }
    }

    override fun handleRelease(player: Player, itemStack: ItemStack, event: ServerboundPlayerActionPacketEvent) {
        if (event.action != ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM) return

        val startTime = playerTimers[player.uniqueId] ?: return
        val currentTime = System.currentTimeMillis()
        val chargeTime = currentTime - startTime

        if (chargeTime >= CHARGE_TIME_MS) {
            // Successfully charged
            completeCharging(player, itemStack)
        } else {
            // Released too early, play fail sound
            player.location.playSoundNearby(Sound.ITEM_CROSSBOW_LOADING_END, 0.5f, 1.2f)
        }

        playerTimers.remove(player.uniqueId)
    }

    private fun startCharging(player: Player, itemStack: ItemStack) {
        playerTimers[player.uniqueId] = System.currentTimeMillis()
        player.location.playSoundNearby(Sound.ITEM_CROSSBOW_LOADING_START, 1f, 1f)

        // Schedule charging sounds
        runTaskTimer(4L,0L){ // 0.2 seconds

            if (playerTimers.containsKey(player.uniqueId)) {
                player.location.playSoundNearby(Sound.ITEM_CROSSBOW_LOADING_MIDDLE, 1f, 1f)
            }
        }
    }

    private fun completeCharging(player: Player, itemStack: ItemStack) {
        val arrowItem = getArrow(player)
        val isCreative = player.gameMode == GameMode.CREATIVE

        if (arrowItem == null && !isCreative) return

        // Store the charged arrow
        val chargedArrow = arrowItem ?: ItemStack(Material.ARROW)
        chargedCrossbows[player.uniqueId] = chargedArrow.clone()

        // Remove arrow from inventory (unless creative or infinity)
        val shouldConsume = player.gameMode == GameMode.SURVIVAL && !itemStack.enchantments.contains(Enchantment.INFINITY)
        if (shouldConsume) {
            removeArrow(player, true)
        }

        // Update crossbow meta to show it's charged
        updateCrossbowMeta(itemStack, chargedArrow)

        player.location.playSoundNearby(Sound.ITEM_CROSSBOW_LOADING_END, 1f, 1f)
    }

    private fun fireCrossbow(player: Player, itemStack: ItemStack) {
        val chargedArrow = chargedCrossbows[player.uniqueId] ?: return

        runTask {
            val arrow: AbstractArrow = when (chargedArrow.type) {
                Material.SPECTRAL_ARROW -> player.launchProjectile(SpectralArrow::class.java)
                Material.TIPPED_ARROW -> {
                    val tippedArrowItem = ItemStack(Material.TIPPED_ARROW)
                    val arrowMeta = chargedArrow.itemMeta as? PotionMeta
                    tippedArrowItem.itemMeta = arrowMeta

                    val tipped = player.launchProjectile(Arrow::class.java)

                    try {
                        tipped.itemStack = tippedArrowItem
                    } catch (e: Exception) {
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

            // Crossbow fires with more power than bow
            arrow.velocity = player.location.direction.multiply(3.15)
            arrow.shooter = player
            arrow.pickupStatus = AbstractArrow.PickupStatus.ALLOWED

            // Apply multishot if present
            val multishotLevel = itemStack.enchantments[Enchantment.MULTISHOT] ?: 0
            if (multishotLevel > 0) {
                // Fire additional arrows at slight angles
                val leftArrow = createMultishotArrow(player, chargedArrow, -10.0)
                val rightArrow = createMultishotArrow(player, chargedArrow, 10.0)
                leftArrow.pickupStatus = AbstractArrow.PickupStatus.CREATIVE_ONLY
                rightArrow.pickupStatus = AbstractArrow.PickupStatus.CREATIVE_ONLY
            }

            player.location.playSoundNearby(Sound.ITEM_CROSSBOW_SHOOT, 1f, 1f)
        }

        // Clear charged state
        chargedCrossbows.remove(player.uniqueId)
        clearCrossbowMeta(itemStack)
    }

    private fun createMultishotArrow(player: Player, arrowItem: ItemStack, angleOffset: Double): AbstractArrow {
        val arrow: AbstractArrow = when (arrowItem.type) {
            Material.SPECTRAL_ARROW -> player.launchProjectile(SpectralArrow::class.java)
            Material.TIPPED_ARROW -> {
                val tipped = player.launchProjectile(Arrow::class.java)
                val arrowMeta = arrowItem.itemMeta as? PotionMeta
                arrowMeta?.let { meta ->
                    meta.customEffects.forEach { effect ->
                        tipped.addCustomEffect(effect, true)
                    }
                    meta.basePotionType?.let { tipped.basePotionType = it }
                }
                tipped
            }
            else -> player.launchProjectile(Arrow::class.java)
        }

        // Calculate angled direction
        val direction = player.location.direction
        val yaw = Math.toRadians(angleOffset)
        val cos = kotlin.math.cos(yaw)
        val sin = kotlin.math.sin(yaw)

        val newX = direction.x * cos - direction.z * sin
        val newZ = direction.x * sin + direction.z * cos

        direction.x = newX
        direction.z = newZ

        arrow.velocity = direction.multiply(3.15)
        arrow.shooter = player

        return arrow
    }

    override fun modifyClientSideStack(player: Player?, server: ItemStack, client: ItemStack): ItemStack {
        return client.withType(Material.CROSSBOW)
    }

    private fun isChargedCrossbow(itemStack: ItemStack): Boolean {
        val meta = itemStack.itemMeta as? CrossbowMeta ?: return false
        return meta.hasChargedProjectiles()
    }

    private fun updateCrossbowMeta(itemStack: ItemStack, arrowItem: ItemStack) {
        val meta = itemStack.itemMeta as? CrossbowMeta ?: return
        meta.setChargedProjectiles(listOf(arrowItem))
        itemStack.itemMeta = meta
    }

    private fun clearCrossbowMeta(itemStack: ItemStack) {
        val meta = itemStack.itemMeta as? CrossbowMeta ?: return
        meta.setChargedProjectiles(listOf())
        itemStack.itemMeta = meta
    }

    private fun getArrow(player: Player): ItemStack? {
        val inv = player.inventory

        // Check offhand first
        val offhandItem = inv.itemInOffHand
        if (isArrow(offhandItem)) {
            val cloned = offhandItem.clone()
            cloned.amount = 1
            return cloned
        }

        // Then check main inventory
        for (i in 0 until inv.size) {
            val item = inv.getItem(i) ?: continue
            if (!isArrow(item)) continue

            val cloned = item.clone()
            cloned.amount = 1
            return cloned
        }

        return null
    }

    private fun removeArrow(player: Player, remove: Boolean): Boolean {
        val inv = player.inventory

        // Check offhand first
        val offhandItem = inv.itemInOffHand
        if (isArrow(offhandItem)) {
            if (remove) {
                offhandItem.amount -= 1
                if (offhandItem.amount <= 0) {
                    inv.setItemInOffHand(null)
                }
            }
            return true
        }

        // Then check main inventory
        for (i in 0 until inv.size) {
            val item = inv.getItem(i) ?: continue
            if (!isArrow(item)) continue

            if (remove) {
                item.amount -= 1
                if (item.amount <= 0) {
                    inv.setItem(i, null)
                }
            }
            return true
        }

        return false
    }

    private fun isArrow(item: ItemStack): Boolean {
        return item.type == Material.ARROW ||
                item.type == Material.TIPPED_ARROW ||
                item.type == Material.SPECTRAL_ARROW
    }
}