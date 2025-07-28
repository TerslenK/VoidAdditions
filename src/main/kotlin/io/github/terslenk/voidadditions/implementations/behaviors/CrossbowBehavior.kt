package io.github.terslenk.voidadditions.implementations.behaviors

import io.github.terslenk.voidadditions.VoidAdditions
import io.github.terslenk.voidadditions.implementations.registry.ProjectileTypes
import io.github.terslenk.voidadditions.implementations.registry.VoidUtils
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
import org.bukkit.entity.*
import org.bukkit.event.block.Action
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.FireworkMeta
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.scheduler.BukkitTask
import xyz.xenondevs.nova.network.event.serverbound.ServerboundPlayerActionPacketEvent
import xyz.xenondevs.nova.util.Key
import xyz.xenondevs.nova.util.item.retrieveData
import xyz.xenondevs.nova.util.item.storeData
import xyz.xenondevs.nova.util.runTask
import xyz.xenondevs.nova.util.runTaskTimer
import xyz.xenondevs.nova.world.item.NovaItem
import xyz.xenondevs.nova.world.item.behavior.ItemBehavior
import xyz.xenondevs.nova.world.item.behavior.ItemBehaviorFactory
import xyz.xenondevs.nova.world.player.WrappedPlayerInteractEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private val CHARGE_KEY = Key(VoidAdditions, "charge_status")
private val STORED_ARROW_KEY = Key(VoidAdditions, "stored_arrow")

class CrossbowBehavior : ItemBehavior {
    private val playerTimers = ConcurrentHashMap<UUID, Long>()
    private val chargeTasks = ConcurrentHashMap<UUID, BukkitTask>()

    companion object : ItemBehaviorFactory<CrossbowBehavior> {
        private const val MAX_CHARGE_TIME_MS = 2000L
        private const val CROSSBOW_POWER = 3.15

        private val CHARGING_CONSUMABLE = Consumable.consumable()
            .animation(ItemUseAnimation.CROSSBOW)
            .sound(SoundEventKeys.INTENTIONALLY_EMPTY)
            .consumeSeconds(Float.MAX_VALUE)
            .hasConsumeParticles(false)
            .build()

        override fun create(item: NovaItem): CrossbowBehavior = CrossbowBehavior()
    }

    override fun handleEquip(player: Player, itemStack: ItemStack, slot: EquipmentSlot, equipped: Boolean, event: EntityEquipmentChangedEvent) {
        if (equipped && getProjectileData(itemStack) == null) {
            setProjectileData(itemStack, ProjectileTypes.NONE)
            player.inventory.setItem(slot, itemStack)
        }
    }

    override fun handleInteract(player: Player, itemStack: ItemStack, action: Action, wrappedEvent: WrappedPlayerInteractEvent) {
        if (!action.isRightClick) return

        val projectileType = getProjectileData(itemStack) ?: ProjectileTypes.NONE
        val playerId = player.uniqueId

        if (projectileType != ProjectileTypes.NONE) {
            cancelChargeTask(playerId)
            fireCrossbow(player, itemStack, projectileType)
            return
        }

        if (playerTimers.containsKey(playerId)) return

        if (VoidUtils.getArrow(player, "CROSSBOW") != null) {
            startCharging(player, itemStack)
        }
    }

    override fun handleRelease(player: Player, itemStack: ItemStack, event: ServerboundPlayerActionPacketEvent) {
        if (event.action != ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM) return
        val playerId = player.uniqueId
        playerTimers.remove(playerId)
        cancelChargeTask(playerId)
        stopCharging(player, itemStack)
    }

    private fun startCharging(player: Player, itemStack: ItemStack) {
        val playerId = player.uniqueId
        val startTime = System.currentTimeMillis()

        itemStack.setData(DataComponentTypes.CONSUMABLE, CHARGING_CONSUMABLE)
        player.updateInventory()
        playerTimers[playerId] = startTime

        val task = runTaskTimer(0, 10) {
            if (playerTimers[playerId] != startTime) {
                chargeTasks.remove(playerId)
                return@runTaskTimer
            }

            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed >= MAX_CHARGE_TIME_MS) {
                chargeTasks.remove(playerId)
                playerTimers.remove(playerId)
                stopCharging(player, itemStack)

                loadProjectile(player, itemStack)
                val projectileType = getProjectileData(itemStack) ?: ProjectileTypes.NONE
                if (projectileType != ProjectileTypes.NONE) {
                    fireCrossbow(player, itemStack, projectileType)
                }
            }
        }

        chargeTasks[playerId] = task
    }

    private fun stopCharging(player: Player, itemStack: ItemStack) {
        itemStack.unsetData(DataComponentTypes.CONSUMABLE)
        runTask { player.updateInventory() }
    }

    private fun loadProjectile(player: Player, itemStack: ItemStack) {
        val arrow = VoidUtils.getArrow(player, "CROSSBOW") ?: return
        val shouldConsume = player.gameMode == GameMode.SURVIVAL
        if (!VoidUtils.removeArrow(player, arrow, "CROSSBOW", shouldConsume)) return

        setStoredArrow(itemStack, arrow)
        val projectileType = getProjectileTypeFromMaterial(arrow.type)
        setProjectileData(itemStack, projectileType)

        runTask {
            player.world.playSound(player.location, Sound.ITEM_CROSSBOW_LOADING_END, 1.0f, 1.0f)
            player.sendMessage("Loaded ${projectileType.name.lowercase().replace('_', ' ')}")
        }
    }

    private fun fireCrossbow(player: Player, itemStack: ItemStack, projectileType: ProjectileTypes) {
        val storedArrow = getStoredArrow(itemStack)
        val multishot = itemStack.itemMeta?.getEnchantLevel(Enchantment.MULTISHOT) ?: 0

        runTask {
            if (multishot > 0) {
                for (i in -1..1) {
                    val yawOffset = i * 5.0
                    fireProjectileWithOffset(player, projectileType, storedArrow, yawOffset, itemStack)
                }
            } else {
                fireProjectileWithOffset(player, projectileType, storedArrow, 0.0, itemStack)
            }
            player.world.playSound(player.location, Sound.ITEM_CROSSBOW_SHOOT, 1.0f, 1.0f)
        }

        clearCrossbowState(itemStack)
        player.sendMessage("Fired ${projectileType.name.lowercase().replace('_', ' ')}")
    }

    private fun fireProjectileWithOffset(player: Player, type: ProjectileTypes, storedArrow: ItemStack?, yawOffset: Double, itemStack: ItemStack) {
        val loc = player.location.clone()
        loc.yaw += yawOffset.toFloat()
        val direction = loc.direction

        val proj = when (type) {
            ProjectileTypes.ARROW, ProjectileTypes.TIPPED_ARROW -> player.launchProjectile(Arrow::class.java, direction)
            ProjectileTypes.SPECTRAL_ARROW -> player.launchProjectile(SpectralArrow::class.java, direction)
            ProjectileTypes.FIREWORK_ROCKET -> player.launchProjectile(Firework::class.java, direction)
            else -> return
        }

        when (proj) {
            is AbstractArrow -> {
                configureArrow(proj, itemStack)
                if (type == ProjectileTypes.TIPPED_ARROW && storedArrow?.itemMeta is PotionMeta) {
                    proj.itemStack = ItemStack(Material.TIPPED_ARROW).apply { itemMeta = storedArrow.itemMeta }
                }
            }
            is Firework -> {
                setFireworkVelocity(proj)
                if (storedArrow?.itemMeta is FireworkMeta) {
                    proj.fireworkMeta = storedArrow.itemMeta as FireworkMeta
                }
            }
        }
    }

    private fun configureArrow(arrow: AbstractArrow, itemStack: ItemStack) {
        setArrowVelocity(arrow)
        arrow.pickupStatus = AbstractArrow.PickupStatus.CREATIVE_ONLY

        val piercing = itemStack.itemMeta?.getEnchantLevel(Enchantment.PIERCING) ?: 0
        if (piercing > 0 && arrow is Arrow) {
            arrow.pierceLevel = piercing
        }
    }

    private fun setArrowVelocity(arrow: AbstractArrow) {
        val velocity = arrow.velocity
        val length = velocity.length()
        if (length > 0) arrow.velocity = velocity.multiply(CROSSBOW_POWER / length)
    }

    private fun setFireworkVelocity(firework: Firework) {
        val velocity = firework.velocity
        val length = velocity.length()
        if (length > 0) firework.velocity = velocity.multiply(CROSSBOW_POWER / length)
    }

    private fun getProjectileTypeFromMaterial(material: Material): ProjectileTypes = when (material) {
        Material.ARROW -> ProjectileTypes.ARROW
        Material.TIPPED_ARROW -> ProjectileTypes.TIPPED_ARROW
        Material.SPECTRAL_ARROW -> ProjectileTypes.SPECTRAL_ARROW
        Material.FIREWORK_ROCKET -> ProjectileTypes.FIREWORK_ROCKET
        else -> ProjectileTypes.ARROW
    }

    private fun cancelChargeTask(playerId: UUID) {
        chargeTasks.remove(playerId)?.cancel()
    }

    private fun clearCrossbowState(itemStack: ItemStack) {
        setProjectileData(itemStack, ProjectileTypes.NONE)
        clearStoredArrow(itemStack)
    }

    private fun getProjectileData(itemStack: ItemStack): ProjectileTypes? = itemStack.retrieveData(CHARGE_KEY)
    private fun setProjectileData(itemStack: ItemStack, type: ProjectileTypes) = itemStack.storeData(CHARGE_KEY, type)
    private fun getStoredArrow(itemStack: ItemStack): ItemStack? = itemStack.retrieveData(STORED_ARROW_KEY)
    private fun setStoredArrow(itemStack: ItemStack, arrow: ItemStack) = itemStack.storeData(STORED_ARROW_KEY, arrow)
    private fun clearStoredArrow(itemStack: ItemStack) = itemStack.storeData(STORED_ARROW_KEY, null)
}
