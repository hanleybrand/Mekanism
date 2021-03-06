package mekanism.common;

import java.util.Map;

import mekanism.common.BlockMachine.MachineType;
import mekanism.common.RecipeHandler.Recipe;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

public class TileEntityOsmiumCompressor extends TileEntityAdvancedElectricMachine
{
	public TileEntityOsmiumCompressor()
	{
		super("Compressor.ogg", "Osmium Compressor", "/mods/mekanism/gui/GuiCompressor.png", Mekanism.osmiumCompressorUsage, 1, 200, MachineType.OSMIUM_COMPRESSOR.baseEnergy, 200);
	}
	
	@Override
	public Map getRecipes()
	{
		return Recipe.OSMIUM_COMPRESSOR.get();
	}

	@Override
	public int getFuelTicks(ItemStack itemstack)
	{
		boolean hasOsmium = false;
		
		for(ItemStack ore : OreDictionary.getOres("ingotOsmium"))
		{
			if(ore.isItemEqual(itemstack))
			{
				hasOsmium = true;
				break;
			}
		}
		
		if(hasOsmium) return 200;
		return 0;
	}
}
