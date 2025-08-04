package io.github.terslenk.voidadditions.implementations.behaviors

import io.github.terslenk.voidadditions.implementations.registry.VoidUtils
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ChargedProjectiles
import io.papermc.paper.datacomponent.item.Consumable
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent
import io.papermc.paper.registry.keys.SoundEventKeys
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Arrow
import org.bukkit.entity.Egg
import org.bukkit.entity.Player
import org.bukkit.entity.Snowball
import org.bukkit.entity.SpectralArrow
import org.bukkit.entity.TNTPrimed
import org.bukkit.entity.Trident
import org.bukkit.event.block.Action
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.FireworkMeta
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.util.Vector
import xyz.xenondevs.nova.network.event.serverbound.ServerboundPlayerActionPacketEvent
import xyz.xenondevs.nova.util.runTaskLater
import xyz.xenondevs.nova.world.item.NovaItem
import xyz.xenondevs.nova.world.item.behavior.ItemBehavior
import xyz.xenondevs.nova.world.item.behavior.ItemBehaviorFactory
import xyz.xenondevs.nova.world.player.WrappedPlayerInteractEvent
import java.util.UUID
import kotlin.math.sin
import kotlin.random.Random

/**
 * Custom behavior class for crossbows with full enchantment support.
 * - Requires holding right-click to charge
 * - Must release and right-click again to fire
 * - Charging based on Quick Charge enchantment
 * - Integrates with custom ammo search/removal via VoidUtils
 * - Supports Multishot, Piercing, Quick Charge, Unbreaking, Tipped Arrows, and Fireworks
 */
class CrossbowBehavior : ItemBehavior {
    private val playerTimers = mutableMapOf<UUID, Long>()

    companion object : ItemBehaviorFactory<CrossbowBehavior> {
        override fun create(item: NovaItem) = CrossbowBehavior()

        private const val BASE_CHARGE_TIME_TICKS = 25
        private const val CHARGE_TIME_NANOS_PER_TICK = 50_000_000L
        private const val ARROW_VELOCITY = 3.15
        private const val FIREWORK_VELOCITY = 1.6
        private const val PROJECTILE_VELOCITY = 2.5
        private const val TNT_FUSE_TICKS = 60 // 3 seconds
        private const val TNT_LAUNCH_SPEED = 3.0
        private const val MULTISHOT_SPREAD = 10.0
        private val CROSSBOW_AMMO = listOf(
            Material.ARROW,
            Material.TIPPED_ARROW,
            Material.SPECTRAL_ARROW,
            Material.FIREWORK_ROCKET
        )
    }

    private fun cleanupTimer(playerId: UUID) {
        playerTimers.remove(playerId)
    }

    /**
     * Calculate charge time based on Quick Charge enchantment level
     */
    private fun getChargeTime(itemStack: ItemStack): Long {
        val quickChargeLevel = itemStack.getEnchantmentLevel(Enchantment.QUICK_CHARGE)
        val reducedTicks = BASE_CHARGE_TIME_TICKS - (quickChargeLevel * 5)
        val finalTicks = maxOf(reducedTicks, 5) // Minimum 5 ticks (0.25 seconds)
        return finalTicks * CHARGE_TIME_NANOS_PER_TICK
    }

    /**
     * Check if item should take durability damage based on Unbreaking enchantment
     */
    private fun shouldTakeDurability(itemStack: ItemStack): Boolean {
        val unbreakingLevel = itemStack.getEnchantmentLevel(Enchantment.UNBREAKING)
        if (unbreakingLevel == 0) return true

        // Unbreaking formula: 1 / (level + 1) chance to take damage
        val chance = 1.0 / (unbreakingLevel + 1)
        return Random.nextDouble() < chance
    }

    /**
     * Apply durability damage to the crossbow
     */
    private fun applyDurability(player: Player, itemStack: ItemStack) {
        if (player.gameMode == GameMode.CREATIVE) return
        if (!shouldTakeDurability(itemStack)) return

        val damageable = itemStack.itemMeta as? org.bukkit.inventory.meta.Damageable ?: return
        val currentDamage = damageable.damage
        val newDamage = currentDamage + 1

        // Check if item would be broken
        if (newDamage >= itemStack.type.maxDurability) {
            // Item breaks
            player.inventory.setItem(player.inventory.heldItemSlot, ItemStack(Material.AIR))
            player.playSound(player.location, "entity.item.break", 1.0f, 1.0f)
        } else {
            // Apply the damage
            damageable.damage = newDamage
            itemStack.itemMeta = damageable
        }
    }

    /**
     * Get the number of projectiles to fire based on Multishot enchantment
     */
    private fun getProjectileCount(itemStack: ItemStack): Int {
        return if (itemStack.containsEnchantment(Enchantment.MULTISHOT)) 3 else 1
    }

    /**
     * Create directional vectors for multishot
     */
    private fun getMultishotDirections(baseDirection: Vector): List<Vector> {
        val directions = mutableListOf<Vector>()

        // Center projectile
        directions.add(baseDirection.clone())

        // Left and right projectiles (10 degrees spread each)
        val spreadRadians = Math.toRadians(MULTISHOT_SPREAD)

        // Calculate perpendicular vector for horizontal spread
        val up = Vector(0, 1, 0)
        val right = baseDirection.clone().crossProduct(up).normalize()

        // Left projectile
        val leftDirection = baseDirection.clone()
        leftDirection.add(right.clone().multiply(-sin(spreadRadians)))
        directions.add(leftDirection.normalize())

        // Right projectile
        val rightDirection = baseDirection.clone()
        rightDirection.add(right.clone().multiply(sin(spreadRadians)))
        directions.add(rightDirection.normalize())

        return directions
    }

    override fun handleEquip(
        player: Player,
        itemStack: ItemStack,
        slot: EquipmentSlot,
        equipped: Boolean,
        event: EntityEquipmentChangedEvent
    ) {
        if (VoidUtils.getItem(player, CROSSBOW_AMMO) != null && itemStack.getData(DataComponentTypes.CHARGED_PROJECTILES) == null) {
            itemStack.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable()
                .animation(ItemUseAnimation.CROSSBOW)
                .consumeSeconds(Float.MAX_VALUE)
                .sound(SoundEventKeys.INTENTIONALLY_EMPTY)
                .hasConsumeParticles(false)
                .build()
            )
        } else {
            itemStack.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable()
                .animation(ItemUseAnimation.NONE)
                .consumeSeconds(Float.MAX_VALUE)
                .sound(SoundEventKeys.INTENTIONALLY_EMPTY)
                .hasConsumeParticles(false)
                .build()
            )
        }
    }

    override fun handleInteract(
        player: Player,
        itemStack: ItemStack,
        action: Action,
        wrappedEvent: WrappedPlayerInteractEvent
    ) {
        val ammoItem = VoidUtils.getItem(player, CROSSBOW_AMMO)
        if (action.isRightClick) {
            val chargedProjectiles = itemStack.getData(DataComponentTypes.CHARGED_PROJECTILES)

            if (ammoItem != null && chargedProjectiles == null) {
                // Start charging
                val projectiles = ChargedProjectiles.chargedProjectiles()
                playerTimers[player.uniqueId] = System.nanoTime()
                val chargeTimeNanos = getChargeTime(itemStack)

                runTaskLater((chargeTimeNanos / CHARGE_TIME_NANOS_PER_TICK + 1)) {
                    val startTime = playerTimers[player.uniqueId] ?: return@runTaskLater
                    val currentTime = System.nanoTime()
                    val charged = (currentTime - startTime) >= chargeTimeNanos
                    val shouldConsume = player.gameMode != GameMode.CREATIVE && !itemStack.enchantments.contains(Enchantment.INFINITY)

                    if (charged) {
                        projectiles.add(ammoItem)
                        // Remove ammo from inventory
                        VoidUtils.removeItem(player, ammoItem, CROSSBOW_AMMO, shouldConsume)
                        itemStack.setData(DataComponentTypes.CHARGED_PROJECTILES, projectiles)
                        itemStack.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable()
                            .animation(ItemUseAnimation.NONE)
                            .consumeSeconds(Float.MAX_VALUE)
                            .sound(SoundEventKeys.INTENTIONALLY_EMPTY)
                            .hasConsumeParticles(false)
                            .build()
                        )
                    } else {
                        cleanupTimer(player.uniqueId)
                    }
                }
            } else if (chargedProjectiles != null) {
                // Fire the projectiles
                val piercingLevel = itemStack.getEnchantmentLevel(Enchantment.PIERCING)
                val projectileCount = getProjectileCount(itemStack)
                val isMultishot = itemStack.containsEnchantment(Enchantment.MULTISHOT)

                chargedProjectiles.projectiles().forEach { projectileItem ->
                    try {
                        val directions = if (isMultishot && projectileCount > 1) {
                            getMultishotDirections(player.location.direction)
                        } else {
                            listOf(player.location.direction)
                        }

                        directions.forEach { direction ->
                            when (projectileItem.type) {
                                Material.ARROW -> {
                                    val projectile = player.launchProjectile(Arrow::class.java)
                                    projectile.velocity = direction.multiply(ARROW_VELOCITY)
                                    // Note: piercingLevel property may not be available in all server versions
                                    try {
                                        val piercingMethod = projectile.javaClass.getMethod("setPiercingLevel", Int::class.java)
                                        piercingMethod.invoke(projectile, piercingLevel)
                                    } catch (e: Exception) {
                                        // Piercing not supported in this server version
                                    }
                                }
                                Material.SPECTRAL_ARROW -> {
                                    val projectile = player.launchProjectile(SpectralArrow::class.java)
                                    projectile.velocity = direction.multiply(ARROW_VELOCITY)
                                    try {
                                        val piercingMethod = projectile.javaClass.getMethod("setPiercingLevel", Int::class.java)
                                        piercingMethod.invoke(projectile, piercingLevel)
                                    } catch (e: Exception) {
                                        // Piercing not supported in this server version
                                    }
                                }
                                Material.TIPPED_ARROW -> {
                                    // Create the arrow item as a proper tipped arrow first
                                    val tippedArrowItem = ItemStack(Material.TIPPED_ARROW)
                                    val arrowMeta = projectileItem.itemMeta as? PotionMeta
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
                                            meta.basePotionType?.let { potionType -> tipped.basePotionType = potionType }
                                        }
                                    }
                                    tipped.velocity = direction.multiply(ARROW_VELOCITY)
                                    try {
                                        val piercingMethod = tipped.javaClass.getMethod("setPiercingLevel", Int::class.java)
                                        piercingMethod.invoke(tipped, piercingLevel)
                                    } catch (e: Exception) {
                                        // Piercing not supported in this server version
                                    }
                                }
                                Material.FIREWORK_ROCKET -> {
                                    // Create the firework item
                                    val fireworkItem = ItemStack(Material.FIREWORK_ROCKET)
                                    val fireworkMeta = projectileItem.itemMeta as? FireworkMeta
                                    fireworkItem.itemMeta = fireworkMeta

                                    // Launch with the firework item
                                    val projectile = player.launchProjectile(Arrow::class.java)

                                    // Some servers have this method
                                    projectile.itemStack = fireworkItem
                                    projectile.velocity = direction.multiply(FIREWORK_VELOCITY)
                                }
                                else -> println("${player.name} tried to launch ${projectileItem.type.name.lowercase()}, but it hasn't been implemented yet.")
                            }
                        }
                    } catch (e: Exception) {
                        println("${player.name} tried to launch ${projectileItem.type.name.lowercase()}, but failed: ${e.message}")
                    }
                }

                // Apply durability damage
                applyDurability(player, itemStack)

                // Clear charged projectiles after firing
                itemStack.resetData(DataComponentTypes.CHARGED_PROJECTILES)
                cleanupTimer(player.uniqueId)
            }
        }
    }

    override fun handleRelease(player: Player, itemStack: ItemStack, event: ServerboundPlayerActionPacketEvent) {
        if (event.action != ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM) return

        val startTime = playerTimers[player.uniqueId] ?: return
        val currentTime = System.nanoTime()
        val chargeTimeNanos = getChargeTime(itemStack)

        // Check if charge time was met for early release detection
        val chargeCompleted = (currentTime - startTime) >= chargeTimeNanos

        if (!chargeCompleted) {
            // Player released early, cancel charging
            cleanupTimer(player.uniqueId)
        }
    }
}