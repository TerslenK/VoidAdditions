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
import org.bukkit.entity.LivingEntity
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.util.Vector
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
import kotlin.random.Random

/**
 * Custom bow behavior implementation that handles bow mechanics including:
 * - Draw time tracking for power calculation
 * - Arrow consumption with infinity enchantment support
 * - Different arrow types (normal, spectral, tipped)
 * - Proper pickup status handling
 * - Full enchantment support (Power, Punch, Flame, Infinity, Unbreaking)
 */
class BowBehavior : ItemBehavior, Listener {

    // Thread-safe map for multiplayer environments
    private val playerDrawTimes = ConcurrentHashMap<UUID, Long>()

    companion object : ItemBehaviorFactory<BowBehavior> {
        override fun create(item: NovaItem) = BowBehavior()

        // Immutable set for better performance on lookups
        private val SUPPORTED_ARROWS = setOf(
            Material.ARROW,
            Material.TIPPED_ARROW,
            Material.SPECTRAL_ARROW
        )

        // Constants for better maintainability
        private const val DRAW_TIME_MULTIPLIER = 500.0
        private const val MAX_POWER_MULTIPLIER = 4.0
        private const val MIN_DRAW_TIME = 0.5
        private const val MAX_CONSUME_SECONDS = Float.MAX_VALUE

        // Enchantment constants
        private const val POWER_DAMAGE_MULTIPLIER = 0.5
        private const val POWER_BASE_BONUS = 0.5
        private const val PUNCH_KNOCKBACK_MULTIPLIER = 0.5
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
            handleUnbreakingDurability(player, itemStack)
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

        // Infinity only works with regular arrows, not tipped or spectral arrows
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

        // Apply enchantment effects
        applyEnchantmentEffects(bow, arrow, powerMultiplier)

        // Set arrow properties
        arrow.velocity = calculateArrowVelocity(player, bow, powerMultiplier)
        arrow.shooter = player

        // Play shooting sound
        player.location.playSoundNearby(Sound.ENTITY_ARROW_SHOOT, 1f, 1f)
    }

    /**
     * Applies all bow enchantment effects to the arrow
     */
    private fun applyEnchantmentEffects(bow: ItemStack, arrow: AbstractArrow, powerMultiplier: Double) {
        val enchantments = bow.enchantments

        // Store Power enchantment level for damage calculation in hit event
        enchantments[Enchantment.POWER]?.let { level ->
            arrow.setMetadata("power_level", org.bukkit.metadata.FixedMetadataValue(
                org.bukkit.Bukkit.getPluginManager().plugins.first(), level
            ))
        }

        // Store Punch enchantment level for knockback calculation in hit event
        enchantments[Enchantment.PUNCH]?.let { level ->
            arrow.setMetadata("punch_level", org.bukkit.metadata.FixedMetadataValue(
                org.bukkit.Bukkit.getPluginManager().plugins.first(), level
            ))
        }

        // Apply Flame enchantment (sets arrow on fire)
        if (enchantments.containsKey(Enchantment.FLAME)) {
            arrow.fireTicks = 100 // 5 seconds of fire
            // Set targets on fire when hit
            arrow.setMetadata("flame_enchanted", org.bukkit.metadata.FixedMetadataValue(
                org.bukkit.Bukkit.getPluginManager().plugins.first(), true
            ))
        }
    }

    /**
     * Calculates arrow velocity including Power enchantment effects
     */
    private fun calculateArrowVelocity(player: Player, bow: ItemStack, powerMultiplier: Double): Vector {
        val baseVelocity = player.location.direction.multiply(powerMultiplier)

        // Power enchantment also affects velocity slightly
        val powerLevel = bow.enchantments[Enchantment.POWER] ?: 0
        val velocityMultiplier = 1.0 + (powerLevel * 0.1) // 10% increase per level

        return baseVelocity.multiply(velocityMultiplier)
    }

    /**
     * Handles Unbreaking enchantment for bow durability
     */
    private fun handleUnbreakingDurability(player: Player, bow: ItemStack) {
        if (player.gameMode == GameMode.CREATIVE) return

        val unbreakingLevel = bow.enchantments[Enchantment.UNBREAKING] ?: 0

        if (unbreakingLevel > 0) {
            // Unbreaking has a chance to not consume durability
            // Formula: 100 / (level + 1) percent chance to take damage
            val damageChance = 100.0 / (unbreakingLevel + 1)
            val randomValue = Random.nextDouble(0.0, 100.0)

            if (randomValue <= damageChance) {
                bow.damage(1, player.world)
            }
            // If randomValue > damageChance, durability is not consumed
        } else {
            // No unbreaking, always take damage
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

    /**
     * Handles flame enchantment effects and punch knockback when arrows hit entities
     */
    @EventHandler
    fun onArrowHit(event: EntityDamageByEntityEvent) {
        val arrow = event.damager as? AbstractArrow ?: return
        val target = event.entity as? LivingEntity ?: return

        // Handle Power enchantment damage bonus
        if (arrow.hasMetadata("power_level")) {
            val powerLevel = arrow.getMetadata("power_level").firstOrNull()?.asInt() ?: 0
            if (powerLevel > 0) {
                val damageBonus = POWER_BASE_BONUS + (powerLevel * POWER_DAMAGE_MULTIPLIER)
                event.damage = event.damage + damageBonus
            }
        }

        // Handle Flame enchantment
        if (arrow.hasMetadata("flame_enchanted")) {
            target.fireTicks = 100 // Set target on fire for 5 seconds
        }

        // Handle Punch enchantment knockback
        if (arrow.hasMetadata("punch_level")) {
            val punchLevel = arrow.getMetadata("punch_level").firstOrNull()?.asInt() ?: 0
            if (punchLevel > 0) {
                // Calculate knockback direction (from arrow to target)
                val knockbackDirection = target.location.subtract(arrow.location).toVector().normalize()
                val knockbackStrength = punchLevel * PUNCH_KNOCKBACK_MULTIPLIER

                // Apply knockback velocity
                target.velocity = target.velocity.add(knockbackDirection.multiply(knockbackStrength))
            }
        }
    }
}