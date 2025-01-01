package io.github.terslenk.voidadditions.implementations.utils

import org.spongepowered.configurate.ConfigurationNode
import xyz.xenondevs.commons.provider.Provider
import java.util.logging.Logger

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
            logger.info("Configuration Loaded from path '$path': $cfgMap")
        } catch (e: Exception) {
            logger.severe("Error loading configuration from path '$path': ${e.message}")
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
            logger.severe("Error loading list from path '$path': ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * Converts any object to a string if possible.
     */
    private fun Any?.toStringOrNull(): String? {
        return this?.toString()
    }
}
