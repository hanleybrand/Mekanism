package mekanism.client;

import java.util.ArrayList;

import mekanism.common.IMechanicalPipe;
import mekanism.common.PipeUtils;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.liquids.LiquidStack;

public class LiquidClientUpdate
{
	/** List of iterated pipes, to prevent infinite loops. */
	public ArrayList<TileEntity> iteratedPipes = new ArrayList<TileEntity>();
	
	/** Pointer pipe of this calculation */
	public TileEntity pointer;
	
	/** Type of liquid to distribute */
	public LiquidStack liquidToSend;

	public LiquidClientUpdate(TileEntity head, LiquidStack liquid)
	{
		pointer = head;
		liquidToSend = liquid;
	}

	public void loopThrough(TileEntity tile)
	{
		if(!iteratedPipes.contains(tile))
		{
			iteratedPipes.add(tile);
		}
		
		TileEntity[] pipes = PipeUtils.getConnectedPipes(tile);
		
		for(TileEntity pipe : pipes)
		{
			if(pipe != null)
			{
				if(!iteratedPipes.contains(pipe))
				{
					loopThrough(pipe);
				}
			}
		}
	}

	public void clientUpdate()
	{
		loopThrough(pointer);
		
		for(TileEntity tileEntity : iteratedPipes)
		{
			if(tileEntity instanceof IMechanicalPipe)
			{
				((IMechanicalPipe)tileEntity).onTransfer(liquidToSend);
			}
		}
	}
}
