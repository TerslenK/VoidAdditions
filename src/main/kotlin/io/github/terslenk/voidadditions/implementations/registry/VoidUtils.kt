package io.github.terslenk.voidadditions.implementations.registry

import io.github.terslenk.voidadditions.VoidAdditions
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.spongepowered.configurate.ConfigurationNode
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.config.Configs
import xyz.xenondevs.nova.util.NamespacedKey
import xyz.xenondevs.nova.util.dropItem
import xyz.xenondevs.nova.util.item.ItemUtils
import java.util.logging.Logger
import kotlin.random.Random

object VoidUtils {
    private val logger: Logger = Logger.getLogger(VoidUtils::class.java.name)
    
    /**
     * Loads a configuration section from a given path and parses it into a map.
     * @param cfg The Provider<ConfigurationNode> instance to fetch configuration from.
     * @param path The custom path to the desired configuration node.
     * @return A map containing the parsed configuration, or an empty map if the node doesn't exist.
     */
    fun loadConfigAsMap(cfg: Provider<ConfigurationNode>, path: String): Map<String, Map<String, String>> {
        val cfgMap = mutableMapOf<String, Map<String, String>>()
        
        try {
            val node = cfg.get().node(*path.split(".").toTypedArray())
            
            if (!node.virtual()) { // Check if node exists
                node.childrenMap().forEach { (key, childNode) ->
                    if (key is String) {
                        val blockType = key.uppercase()
                        val properties = childNode.childrenMap().mapNotNull { (propKey, propValue) ->
                            propKey.toStringOrNull()?.let { it to propValue.string.orEmpty() }
                        }.toMap()
                        cfgMap[blockType] = properties
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return cfgMap
    }
    
    /**
     * Loads a list of strings from a given path in the configuration.
     * @param cfg The Provider<ConfigurationNode> instance to fetch configuration from.
     * @param path The custom path to the desired configuration node.
     * @return A list of strings, or an empty list if the node doesn't exist or is invalid.
     */
    fun loadConfigAsList(cfg: Provider<ConfigurationNode>, path: String): List<String> {
        return try {
            val node = cfg.get().node(*path.split(".").toTypedArray())
            if (!node.virtual() && node.isList) {
                node.childrenList().mapNotNull { it.string?.uppercase() }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    @JvmName("encodeToIntVararg")
    fun encodeToInt(vararg booleans: Boolean): Int =
        encodeToInt(booleans)

    @JvmName("encodeToIntArray")
    fun encodeToInt(booleans: BooleanArray): Int {
        var result = 0
        for (i in booleans.indices) {
            if (booleans[i]) {
                result = result or (1 shl i)
            }
        }
        return result
    }

    fun addModifier(
        player: Player,
        attribute: Attribute,
        keyName: String,
        amount: Double,
        operation: AttributeModifier.Operation
    ) {
        val instance = player.getAttribute(attribute) ?: return
        val key = NamespacedKey(VoidAdditions, keyName)

        instance.modifiers
            .filter { it.key == key }
            .forEach { instance.removeModifier(it) }

        val modifier = AttributeModifier(key, amount, operation)
        instance.addModifier(modifier)
    }

    fun hasModifier(
        player: Player,
        attribute: Attribute,
        keyName: String
    ): Boolean {
        val instance = player.getAttribute(attribute) ?: return false
        val key = NamespacedKey(VoidAdditions, keyName)

        return instance.modifiers.any { it.key == key }
    }

    fun removeModifier(
        player: Player,
        attribute: Attribute,
        keyName: String
    ) {
        val instance = player.getAttribute(attribute) ?: return
        val key = NamespacedKey(VoidAdditions, keyName)

        instance.modifiers
            .filter { it.key == key }
            .forEach { instance.removeModifier(it) }
    }

    fun handleBlockReplacement(event: BlockBreakEvent, replaceWith: String, dropItem: String, chance: Int) {
        val player = event.player // Get the player who broke the block
        val location = event.block.location // Get the block's location

        when (replaceWith.uppercase()) {
            "NOTHING" -> {
                val random = Random.nextInt(0,100)
                // Play the breaking sound of the original block
                if (random < chance) {
                    event.block.location.dropItem(ItemUtils.getItemStack(dropItem))
                    player.sendMessage(random.toString())
                }
                // Play the breaking sound of the original block
                player.playSound(location, event.block.blockData.soundGroup.breakSound, 1.0f, 1.0f)
            }
            else -> {
                val newBlockType = Material.matchMaterial(replaceWith)
                val random = Random.nextInt(0,100)

                if (newBlockType != null) {
                    event.isCancelled = true
                    event.block.type = newBlockType
                    if (random < chance) {
                        event.block.location.dropItem(ItemUtils.getItemStack(dropItem))
                        player.sendMessage(random.toString())
                    }
                    // Play the breaking sound of the original block
                    player.playSound(location, event.block.blockData.soundGroup.breakSound, 1.0f, 1.0f)
                } else {
                    println("Invalid replacement block type")
                }
            }
        }
    }

    fun mergeTags(tag: String): MutableSet<Any> {
        val newSet = mutableSetOf<Any>()

        tag.split(" ").forEach {
            tag -> val values = Configs["tags/$tag"].toString(); newSet.add(values)
        }

        return newSet
    }


    /**
     * Converts any object to a string if possible.
     */
    private fun Any?.toStringOrNull(): String? {
        return this?.toString()
    }
}
