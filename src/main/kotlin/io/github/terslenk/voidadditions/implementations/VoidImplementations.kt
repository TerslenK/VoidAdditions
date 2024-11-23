package io.github.terslenk.voidadditions.implementations

import io.github.terslenk.voidadditions.VoidAdditions
import io.github.terslenk.voidadditions.implementations.abilities.LimitedMiningAbility
import io.github.terslenk.voidadditions.implementations.behaviors.CavemanTool
import xyz.xenondevs.nova.addon.registry.AbilityTypeRegistry
import xyz.xenondevs.nova.addon.registry.BlockRegistry
import xyz.xenondevs.nova.addon.registry.ItemRegistry
import xyz.xenondevs.nova.initialize.Init
import xyz.xenondevs.nova.initialize.InitStage
import xyz.xenondevs.nova.resources.layout.block.BackingStateCategory
import xyz.xenondevs.nova.world.item.behavior.Damageable
import xyz.xenondevs.nova.world.item.behavior.Fuel
import xyz.xenondevs.nova.world.item.behavior.Tool
import xyz.xenondevs.nova.world.item.tool.VanillaToolTiers

// Items
@Init(stage = InitStage.PRE_PACK)
object VoidItems: ItemRegistry by VoidAdditions.registry {
    // Ores
    val LEAD_ORE = item(block = VoidBlocks.LEAD_ORE) {}
    val OSMIUM_ORE = item(block = VoidBlocks.OSMIUM_ORE) {}
    val TIN_ORE = item(block = VoidBlocks.TIN_ORE) {}
    val NICKEL_ORE = item(block = VoidBlocks.NICKEL_ORE) {}
    val SILVER_ORE = item(block = VoidBlocks.SILVER_ORE) {}
    val ZINC_ORE = item(block = VoidBlocks.ZINC_ORE) {}
    
    // Ingots
    val LEAD_INGOT = registerItem("lead_ingot", localizedName = "Lead Ingot")
    val OSMIUM_INGOT = registerItem("osmium_ingot", localizedName = "Osmium Ingot")
    val TIN_INGOT = registerItem("tin_ingot", localizedName = "Tin Ingot")
    val NICKEL_INGOT = registerItem("nickel_ingot", localizedName = "Nickel Ingot")
    val SILVER_INGOT = registerItem("silver_ingot", localizedName = "Silver Ingot")
    val ZINC_INGOT = registerItem("zinc_ingot", localizedName = "Zinc Ingot")
    
    //AGE ONE
    val PLANT_FIBER = registerItem("plant_fiber", localizedName = "Plant Fiber")
    val PLANT_STRING = registerItem("plant_string", localizedName = "Plant String")
    val STONE_PEBBLE = registerItem("stone_pebble", localizedName = "Stone Pebble")
    val POINTY_STICK = item("pointy_stick") {
        localizedName("Pointy Stick")
        behaviors(Tool,Damageable,Fuel,CavemanTool)
        maxStackSize(1)
        VanillaToolTiers
    }
    val POINTY_STONE = item("pointy_stone") {
        localizedName("Pointy Stone")
        behaviors(Tool,Damageable,CavemanTool)
        maxStackSize(1)
        VanillaToolTiers
    }
    val OAK_BARK = registerItem("oak_bark", localizedName = "Oak Bark")
    val SPRUCE_BARK = registerItem("spruce_bark", localizedName = "Spruce Bark")
    val BIRCH_BARK = registerItem("birch_bark", localizedName = "Birch Bark")
    val JUNGLE_BARK = registerItem("jungle_bark", localizedName = "Jungle Bark")
    val ACACIA_BARK = registerItem("acacia_bark", localizedName = "Acacia Bark")
    val DARK_OAK_BARK = registerItem("dark_oak_bark", localizedName = "Dark Oak Bark")
    val MANGROVE_BARK = registerItem("mangrove_bark", localizedName = "Mangrove Bark")
    val CHERRY_BARK = registerItem("cherry_bark", localizedName = "Cherry Bark")
}

// Blocks
@Init(stage = InitStage.PRE_PACK)
object VoidBlocks: BlockRegistry by VoidAdditions.registry {
    val LEAD_ORE = block("lead_ore") {
        models {
            stateBacked( BackingStateCategory.MUSHROOM_BLOCK )
            selectModel { defaultModel.rotated() }
        }
    }
    val OSMIUM_ORE = block("osmium_ore") {
        models {
            stateBacked( BackingStateCategory.MUSHROOM_BLOCK )
            selectModel { defaultModel.rotated() }
        }
    }
    val TIN_ORE = block("tin_ore") {
        models {
            stateBacked( BackingStateCategory.MUSHROOM_BLOCK )
            selectModel { defaultModel.rotated() }
        }
    }
    val NICKEL_ORE = block("nickel_ore") {
        models {
            stateBacked( BackingStateCategory.MUSHROOM_BLOCK )
            selectModel { defaultModel.rotated() }
        }
    }
    val SILVER_ORE = block("silver_ore") {
        models {
            stateBacked( BackingStateCategory.MUSHROOM_BLOCK )
            selectModel { defaultModel.rotated() }
        }
    }
    val ZINC_ORE = block("zinc_ore") {
        models {
            stateBacked( BackingStateCategory.MUSHROOM_BLOCK )
            selectModel { defaultModel.rotated() }
        }
    }
}

// Abilities
@Init(stage = InitStage.PRE_PACK)
object VoidAbilities : AbilityTypeRegistry by VoidAdditions.registry {
    val LIMITED_MINING = registerAbilityType("limited_mining", ::LimitedMiningAbility)
}