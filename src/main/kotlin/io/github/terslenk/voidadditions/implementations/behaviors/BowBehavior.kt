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
import java.util.concurrent.ConcurrentHashMap

/**
 * Custom bow behavior implementation that handles bow mechanics including:
 * - Draw time tracking for power calculation
 * - Arrow consumption with infinity enchantment support
 * - Different arrow types (normal, spectral, tipped)
 * - Proper pickup status handling
 */
class BowBehavior : ItemBehavior {

    // Thread-safe map for multiplayer environments
    private val playerDrawTimes = ConcurrentHashMap<UUID, Long>()

    companion object : ItemBehaviorFactory<BowBehavior> {
        override fun create(item: NovaItem) = BowBehavior()

        // Immutable set for better performance on lookups
        private val SUPPORTED_ARROWS = mutableListOf<Material>(
            Material.ARROW,
            Material.TIPPED_ARROW,
            Material.SPECTRAL_ARROW
        )

        // Constants for better maintainability
        private const val DRAW_TIME_MULTIPLIER = 500.0
        private const val MAX_POWER_MULTIPLIER = 4.0
        private const val MIN_DRAW_TIME = 0.5
        private const val MAX_CONSUME_SECONDS = Float.MAX_VALUE
    }

    override fun handleInteract(
        player: Player,
        itemStack: ItemStack,
        action: Action,
        wrappedEvent: WrappedPlayerInteractEvent
    ) {
        if (!action.isRightClick) return

        val hasAmmo = hasValidAmmo(player)
        updateConsumableComponent(itemStack, hasAmmo)
        player.updateInventory()

        if (hasAmmo || player.gameMode == GameMode.CREATIVE) {
            playerDrawTimes[player.uniqueId] = System.currentTimeMillis()
        }
    }

    override fun handleRelease(player: Player, itemStack: ItemStack, event: ServerboundPlayerActionPacketEvent) {
        if (event.action != ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM) return

        val drawTime = calculateDrawTime(player) ?: return
        val powerMultiplier = calculatePowerMultiplier(drawTime)

        // Don't shoot if draw time is too short
        if (powerMultiplier <= MIN_DRAW_TIME) {
            cleanupPlayerState(player)
            return
        }

        val arrowItem = VoidUtils.getItem(player, SUPPORTED_ARROWS.toMutableList())
        val isCreative = player.gameMode == GameMode.CREATIVE

        // Check ammunition requirements
        if (!isCreative && arrowItem == null) {
            cleanupPlayerState(player)
            return
        }

        // Handle arrow consumption
        val shouldConsumeArrow = shouldConsumeAmmo(player, itemStack, arrowItem)
        if (shouldConsumeArrow) {
            VoidUtils.removeItem(player, itemStack, SUPPORTED_ARROWS.toMutableList(), true)
        }

        // Shoot the arrow asynchronously to avoid blocking
        runTask {
            shootArrow(player, itemStack, arrowItem, powerMultiplier, shouldConsumeArrow)
        }

        cleanupPlayerState(player)
    }

    override fun modifyClientSideStack(player: Player?, server: ItemStack, client: ItemStack): ItemStack {
        return client.withType(Material.BOW)
    }

    /**
     * Checks if the player has valid ammunition for the bow
     */
    private fun hasValidAmmo(player: Player): Boolean {
        return VoidUtils.getItem(player, SUPPORTED_ARROWS.toMutableList()) != null
    }

    /**
     * Updates the consumable component based on ammunition availability
     */
    private fun updateConsumableComponent(itemStack: ItemStack, hasAmmo: Boolean) {
        val animation = if (hasAmmo) ItemUseAnimation.BOW else ItemUseAnimation.NONE

        itemStack.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable()
            .animation(animation)
            .sound(SoundEventKeys.INTENTIONALLY_EMPTY)
            .consumeSeconds(MAX_CONSUME_SECONDS)
            .hasConsumeParticles(false)
            .build())
    }

    /**
     * Calculates the time the bow has been drawn
     */
    private fun calculateDrawTime(player: Player): Long? {
        val startTime = playerDrawTimes[player.uniqueId] ?: return null
        return System.currentTimeMillis() - startTime
    }

    /**
     * Calculates the power multiplier based on draw time
     */
    private fun calculatePowerMultiplier(drawTime: Long): Double {
        return (drawTime / DRAW_TIME_MULTIPLIER).coerceAtMost(MAX_POWER_MULTIPLIER)
    }

    /**
     * Determines if ammunition should be consumed
     */
    private fun shouldConsumeAmmo(player: Player, bow: ItemStack, arrowItem: ItemStack?): Boolean {
        if (player.gameMode != GameMode.SURVIVAL) return false

        val hasInfinity = bow.enchantments.containsKey(Enchantment.INFINITY)
        val isRegularArrow = arrowItem?.type == Material.ARROW

        return !(hasInfinity && isRegularArrow)
    }

    /**
     * Creates and launches the appropriate arrow type
     */
    private fun shootArrow(
        player: Player,
        bow: ItemStack,
        arrowItem: ItemStack?,
        powerMultiplier: Double,
        shouldConsumeArrow: Boolean
    ) {
        val arrow = createArrow(player, arrowItem, shouldConsumeArrow)

        // Set arrow properties
        arrow.velocity = player.location.direction.multiply(powerMultiplier)
        arrow.shooter = player

        // Play shooting sound
        player.location.playSoundNearby(Sound.ENTITY_ARROW_SHOOT, 1f, 1f)

        // Damage bow in survival mode
        if (player.gameMode != GameMode.CREATIVE) {
            bow.damage(1, player.world)
        }
    }

    /**
     * Creates the appropriate arrow type based on the ammunition
     */
    private fun createArrow(player: Player, arrowItem: ItemStack?, shouldConsumeArrow: Boolean): AbstractArrow {
        return when (arrowItem?.type) {
            Material.SPECTRAL_ARROW -> {
                player.launchProjectile(SpectralArrow::class.java).apply {
                    pickupStatus = if (shouldConsumeArrow)
                        AbstractArrow.PickupStatus.ALLOWED
                    else
                        AbstractArrow.PickupStatus.CREATIVE_ONLY
                }
            }

            Material.TIPPED_ARROW -> {
                createTippedArrow(player, arrowItem, shouldConsumeArrow)
            }

            else -> { // Material.ARROW or fallback
                player.launchProjectile(Arrow::class.java).apply {
                    pickupStatus = if (shouldConsumeArrow)
                        AbstractArrow.PickupStatus.ALLOWED
                    else
                        AbstractArrow.PickupStatus.CREATIVE_ONLY
                }
            }
        }
    }

    /**
     * Creates a tipped arrow with proper potion effects
     */
    private fun createTippedArrow(player: Player, arrowItem: ItemStack, shouldConsumeArrow: Boolean): Arrow {
        val arrow = player.launchProjectile(Arrow::class.java)
        arrow.pickupStatus = if (shouldConsumeArrow)
            AbstractArrow.PickupStatus.ALLOWED
        else
            AbstractArrow.PickupStatus.CREATIVE_ONLY

        // Apply potion effects
        val potionMeta = arrowItem.itemMeta as? PotionMeta
        potionMeta?.let { meta ->
            // Try to set the item stack directly (newer server versions)
            try {
                val tippedArrowItem = ItemStack(Material.TIPPED_ARROW).apply {
                    itemMeta = meta
                }
                arrow.itemStack = tippedArrowItem
            } catch (e: Exception) {
                // Fallback: manually copy effects for older versions
                copyPotionEffects(arrow, meta)
            }
        }

        return arrow
    }

    /**
     * Manually copies potion effects to the arrow (fallback method)
     */
    private fun copyPotionEffects(arrow: Arrow, potionMeta: PotionMeta) {
        // Copy custom effects
        potionMeta.customEffects.forEach { effect ->
            arrow.addCustomEffect(effect, true)
        }

        // Copy base potion type
        potionMeta.basePotionType?.let { baseType ->
            arrow.basePotionType = baseType
        }
    }

    /**
     * Cleans up player-specific state
     */
    private fun cleanupPlayerState(player: Player) {
        playerDrawTimes.remove(player.uniqueId)
    }
}