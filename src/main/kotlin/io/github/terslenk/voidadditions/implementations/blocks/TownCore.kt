package io.github.terslenk.voidadditions.implementations.blocks

import org.bukkit.entity.Player
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.block.tileentity.TileEntity
import xyz.xenondevs.nova.world.block.tileentity.menu.TileEntityMenuClass
import xyz.xenondevs.nova.world.region.Region

class TownCore(pos: BlockPos, blockState: NovaBlockState, data: Compound) : TileEntity(pos, blockState, data) {
    
    private val region = storedRegion(
        "region",
        minSize = provider(1),
        maxSize = provider(10),
        defaultSize = 5,
        createRegion = { size -> Region.surrounding(pos, size) }
    )
    
    @TileEntityMenuClass
    inner class TownCoreMenu(player: Player) : IndividualTileEntityMenu(player) {
        
        override val gui = Gui.normal()
            .setStructure(
                "v # # # # # # # #",
                "# # + # d # - # #",
                "# # # # # # # # #",
                "# # # # # # # # #",
                "# # # # # # # # #"
                )
            .addIngredient('+', region.increaseSizeItem)
            .addIngredient('-', region.decreaseSizeItem)
            .addIngredient('d', region.displaySizeItem)
            .addIngredient('v', region.createVisualizeRegionItem(player))
            .build()
        
    }
    
}