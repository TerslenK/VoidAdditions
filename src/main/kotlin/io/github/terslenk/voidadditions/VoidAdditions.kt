package io.github.terslenk.voidadditions

import org.bukkit.Material
import org.bukkit.event.Listener
import org.spongepowered.configurate.serialize.Scalars
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.config.Configs
import xyz.xenondevs.nova.util.item.ItemUtils
import xyz.xenondevs.nova.world.item.NovaItem

object VoidAdditions : Addon() {
    
    override fun init() {
        // Called when the addon is initialized.
        println("Addon is initialized")
    }
    
    override fun onEnable() {
        // Called when the addon is enabled.
        val whitelistedBlocks = Configs["void_additions:main_config"].entry<HashMap<Material, MaterialValues>>("modded_whitelisted_blocks").get()
        
        if (whitelistedBlocks.isNotEmpty()) {
            whitelistedBlocks.forEach {
                val targetBlock = Configs["void_additions:main_config"].entry<Material>("modded_whitelisted_blocks", it.toString()).get()
                val desiredBlock = Configs["void_additions:main_config"].entry<Material>("modded_whitelisted_blocks", it.toString(), "replace_with").get()
                val droppedItem = ItemUtils.toItemStack(Configs["void_additions:main_config"].entry<String>("modded_whitelisted_blocks", it.toString(), "drops").toString()).setAmount(Configs["void_additions:main_config"].entry<Int>("modded_whitelisted_blocks", it.toString(), "amount").get())
                
                println("Found $targetBlock, breaking it would replace it with $desiredBlock and would drop $droppedItem")
            }
        } else {
            println("No materials found")
        }
    }
    
    override fun onDisable() {
        // Called when the addon is disabled.
        println("Addon Has successfully disabled")
    }
    
    class MaterialValues(val desiredBlock: Material, val droppedItem: String, val amount: Int, val breakSpeed: Double)
}