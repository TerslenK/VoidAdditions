package io.github.terslenk.voidadditions.implementations.abilities

import io.github.terslenk.voidadditions.VoidAdditions
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import xyz.xenondevs.nova.config.Configs
import xyz.xenondevs.nova.util.data.NamespacedKey
import xyz.xenondevs.nova.world.item.tool.ToolCategory
import xyz.xenondevs.nova.world.item.tool.VanillaToolCategories
import xyz.xenondevs.nova.world.player.ability.Ability


class LimitedMiningAbility(player: Player) : Ability(player) {
    
    override fun handleRemove() {
        player.sendMessage("Abilities removed from ${player.name}")
    }
    
    override fun handleTick() {
        val whitelistedBlocks = Configs["void_additions:main_config"].entry<MutableList<Material>>("whitelisted_blocks").get()
        val key = NamespacedKey(VoidAdditions, "limited_mining_ability")
        val instance = player.getAttribute(Attribute.PLAYER_BLOCK_BREAK_SPEED)
        
        if (((VanillaToolCategories.AXE !in ToolCategory.ofItem(player.inventory.itemInMainHand)) || (VanillaToolCategories.PICKAXE !in ToolCategory.ofItem(player.inventory.itemInMainHand)) || (VanillaToolCategories.SHOVEL !in ToolCategory.ofItem(player.inventory.itemInMainHand)) || !whitelistedBlocks.contains(player.getTargetBlock(null, player.getAttribute(Attribute.PLAYER_BLOCK_INTERACTION_RANGE)?.value?.toInt() ?: 7).type)) && (player.gameMode == GameMode.SURVIVAL)) {
            instance?.modifiers?.filterNotNull()?.filter { m -> m.key == key || m.name == player.name }?.forEach { instance.removeModifier(it) }
            instance?.addModifier(AttributeModifier(key, Configs["void_additions:main_config"].entry<Double>("player_break_speed").get(), AttributeModifier.Operation.ADD_SCALAR))
        }
        if ((VanillaToolCategories.AXE in ToolCategory.ofItem(player.inventory.itemInMainHand) || VanillaToolCategories.PICKAXE in ToolCategory.ofItem(player.inventory.itemInMainHand) || VanillaToolCategories.SHOVEL in ToolCategory.ofItem(player.inventory.itemInMainHand) || whitelistedBlocks.contains(player.getTargetBlock(null, player.getAttribute(Attribute.PLAYER_BLOCK_INTERACTION_RANGE)?.value?.toInt() ?: 7).type)) && player.gameMode == GameMode.ADVENTURE) {
            instance?.modifiers?.filterNotNull()?.filter { m -> m.key == key || m.name == player.name }?.forEach { instance.removeModifier(it) }
        }
        
        if (instance != null) {
            player.sendMessage("Block Break Speed: ${instance.value}")
        }
    }
}