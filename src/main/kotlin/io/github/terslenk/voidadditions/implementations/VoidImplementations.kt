@file:Suppress("unused")

package io.github.terslenk.voidadditions.implementations

import io.github.terslenk.voidadditions.VoidAdditions
import io.github.terslenk.voidadditions.VoidAdditions.registerAbilityType
import io.github.terslenk.voidadditions.VoidAdditions.registerItem
import io.github.terslenk.voidadditions.implementations.abilities.LimitedMiningAbility
import io.github.terslenk.voidadditions.implementations.behaviors.CavemanTool
import io.github.terslenk.voidadditions.implementations.behaviors.CrossbowBehavior
import io.github.terslenk.voidadditions.implementations.behaviors.BowBehavior
import io.github.terslenk.voidadditions.implementations.network.StressHolder
import io.github.terslenk.voidadditions.implementations.network.StressNetwork
import io.github.terslenk.voidadditions.implementations.network.StressNetworkGroup
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.nova.initialize.Init
import xyz.xenondevs.nova.initialize.InitStage
import xyz.xenondevs.nova.resources.builder.layout.item.ChargedType
import xyz.xenondevs.nova.resources.builder.layout.item.ConditionItemModelProperty
import xyz.xenondevs.nova.resources.builder.layout.item.RangeDispatchItemModelProperty
import xyz.xenondevs.nova.resources.builder.layout.item.SelectItemModelProperty
import xyz.xenondevs.nova.world.item.behavior.Damageable
import xyz.xenondevs.nova.world.item.behavior.Enchantable
import xyz.xenondevs.nova.world.item.behavior.Fuel
import xyz.xenondevs.nova.world.item.behavior.Shield

// Items
@Init(stage = InitStage.PRE_PACK)
object VoidItems {
    // Ores
    // val LEAD_ORE = VoidAdditions.item(block = VoidBlocks.LEAD_ORE) {}
    // val OSMIUM_ORE = VoidAdditions.item(block = VoidBlocks.OSMIUM_ORE) {}
    // val TIN_ORE = VoidAdditions.item(block = VoidBlocks.TIN_ORE) {}
    // val NICKEL_ORE = VoidAdditions.item(block = VoidBlocks.NICKEL_ORE) {}
    // val SILVER_ORE = VoidAdditions.item(block = VoidBlocks.SILVER_ORE) {}
    // val ZINC_ORE = VoidAdditions.item(block = VoidBlocks.ZINC_ORE) {}

    // Ingots
    val LEAD_INGOT = registerItem("lead_ingot")
    val OSMIUM_INGOT = registerItem("osmium_ingot")
    val TIN_INGOT = registerItem("tin_ingot")
    val NICKEL_INGOT = registerItem("nickel_ingot")
    val SILVER_INGOT = registerItem("silver_ingot")
    val ZINC_INGOT = registerItem("zinc_ingot")

    //AGE ONE
    val PLANT_FIBER = registerItem("plant_fiber")
    val PLANT_STRING = registerItem("plant_string")
    val STONE_PEBBLE = registerItem("stone_pebble")
    val POINTY_STICK = registerItem("pointy_stick", Damageable(), Fuel(), CavemanTool())
    val POINTY_STONE = registerItem("pointy_stone", Damageable(), CavemanTool)

    val CUSTOM_BOW = VoidAdditions.item("custom_bow") {
        modelDefinition {
            model = condition(ConditionItemModelProperty.UsingItem) {
                onFalse = buildModel { getModel("item/custom_bow/bow_standby") }
                onTrue = rangeDispatch(RangeDispatchItemModelProperty.UseDuration(false)) {
                    entry[20] = { getModel("item/custom_bow/bow_pulling_2") }
                    entry[10] = { getModel("item/custom_bow/bow_pulling_1") }
                    entry[0] = { getModel("item/custom_bow/bow_pulling_0") }
                }
            }
        }
        behaviors(
            BowBehavior, Damageable(), Enchantable(), Fuel()
        )
        maxStackSize(1)
    }

    val CUSTOM_CROSSBOW = VoidAdditions.item("custom_crossbow") {
        modelDefinition {
            model = select(SelectItemModelProperty.ChargedType) {
                case[ChargedType.NONE] = condition(ConditionItemModelProperty.UsingItem) {
                    onFalse = buildModel { getModel("item/custom_crossbow/crossbow_standby") }
                    onTrue = rangeDispatch(RangeDispatchItemModelProperty.UseDuration(false)) {
                        entry[20] = { getModel("item/custom_crossbow/crossbow_pulling_2") }
                        entry[10] = { getModel("item/custom_crossbow/crossbow_pulling_1") }
                        entry[0] = { getModel("item/custom_crossbow/crossbow_pulling_0") }
                    }
                }
                case[ChargedType.ARROW] = { getModel("item/custom_crossbow/crossbow_arrow") }
                case[ChargedType.ROCKET] = { getModel("item/custom_crossbow/crossbow_firework") }
            }
        }
        behaviors(
            CrossbowBehavior, Damageable(), Enchantable(), Fuel()
        )
        maxStackSize(1)
    }

    val CUSTOM_SHIELD = VoidAdditions.item("custom_shield") {
        modelDefinition {
            model = shieldSpecialModel {

            }
        }
        behaviors(
            Shield(), Damageable(), Enchantable(), Fuel()
        )
        maxStackSize(1)
    }
}

// Blocks
@Init(stage = InitStage.PRE_PACK)
object VoidBlocks

// Abilities
@Init(stage = InitStage.PRE_PACK)
object VoidAbilities {
    val LIMITED_MINING = registerAbilityType("limited_mining", ::LimitedMiningAbility)
}

@Init(stage = InitStage.PRE_PACK)
object VoidNetwork {
    val STRESS_NETWORK = VoidAdditions.registerNetworkType(
        name = "stress_network",
        createNetwork = ::StressNetwork,
        createGroup = ::StressNetworkGroup,
        validateLocal = StressNetwork::validateLocal,
        tickDelay = provider(5),
        holderTypes = arrayOf(StressHolder::class)
    )
}