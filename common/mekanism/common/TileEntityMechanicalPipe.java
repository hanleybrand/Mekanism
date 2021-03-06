package mekanism.common;

import java.util.ArrayList;
import java.util.Arrays;

import mekanism.api.Object3D;
import mekanism.common.PacketHandler.Transmission;
import mekanism.common.network.PacketDataRequest;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.liquids.ILiquidTank;
import net.minecraftforge.liquids.ITankContainer;
import net.minecraftforge.liquids.LiquidContainerRegistry;
import net.minecraftforge.liquids.LiquidStack;
import net.minecraftforge.liquids.LiquidTank;

import com.google.common.io.ByteArrayDataInput;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class TileEntityMechanicalPipe extends TileEntity implements IMechanicalPipe, ITankContainer, ITileNetwork
{
	/** The fake tank used for liquid transfer calculations. */
	public LiquidTank dummyTank = new LiquidTank(LiquidContainerRegistry.BUCKET_VOLUME);
	
	/** The LiquidStack displayed on this pipe. */
	public LiquidStack refLiquid = null;
	
	/** The liquid network currently in use by this pipe segment. */
	public LiquidNetwork liquidNetwork;
	
	/** This pipe's active state. */
	public boolean isActive = false;
	
	/** The scale (0F -> 1F) of this pipe's liquid level. */
	public float liquidScale;
	
	@Override
	public void onTransfer(LiquidStack liquidStack)
	{
		if(liquidStack.isLiquidEqual(refLiquid))
		{
			liquidScale = Math.min(1, liquidScale+((float)liquidStack.amount/50F));
		}
		else if(refLiquid == null)
		{
			refLiquid = liquidStack.copy();
			liquidScale += Math.min(1, ((float)liquidStack.amount/50F));
		}
	}
	
	@Override
	public LiquidNetwork getNetwork()
	{
		if(liquidNetwork == null)
		{
			liquidNetwork = new LiquidNetwork(this);
		}
		
		return liquidNetwork;
	}
	
	@Override
	public void invalidate()
	{
		if(!worldObj.isRemote)
		{
			getNetwork().split(this);
		}
		
		super.invalidate();
	}
	
	@Override
	public void setNetwork(LiquidNetwork network)
	{
		liquidNetwork = network;
	}
	
	@Override
	public void refreshNetwork() 
	{
		if(!worldObj.isRemote)
		{
			for(ForgeDirection side : ForgeDirection.VALID_DIRECTIONS)
			{
				TileEntity tileEntity = Object3D.get(this).getFromSide(side).getTileEntity(worldObj);
				
				if(tileEntity instanceof IMechanicalPipe)
				{
					getNetwork().merge(((IMechanicalPipe)tileEntity).getNetwork());
				}
			}
			
			getNetwork().refresh();
		}
	}
	
	@Override
	public void updateEntity()
	{
		if(worldObj.isRemote)
		{
			if(liquidScale > 0)
			{
				liquidScale -= .01;
			}
			else {
				refLiquid = null;
			}
		}	
		else {		
			if(isActive)
			{
				ITankContainer[] connectedAcceptors = PipeUtils.getConnectedAcceptors(this);
				
				for(ITankContainer container : connectedAcceptors)
				{
					ForgeDirection side = ForgeDirection.getOrientation(Arrays.asList(connectedAcceptors).indexOf(container));
					
					if(container != null)
					{
						LiquidStack received = container.drain(side, 100, false);
						
						if(received != null && received.amount != 0)
						{
							container.drain(side, getNetwork().emit(received, true, Object3D.get(this).getFromSide(side).getTileEntity(worldObj)), true);
						}
					}
				}
			}
		}
	}
	
	@Override
	public boolean canUpdate()
	{
		return true;
	}
	
	@Override
	public void validate()
	{
		super.validate();
		
		if(worldObj.isRemote)
		{
			PacketHandler.sendPacket(Transmission.SERVER, new PacketDataRequest().setParams(Object3D.get(this)));
		}
	}
	
	@Override
	public void handlePacketData(ByteArrayDataInput dataStream)
	{
		isActive = dataStream.readBoolean();
	}
	
	@Override
	public ArrayList getNetworkedData(ArrayList data)
	{
		data.add(isActive);
		return data;
	}
	
	@Override
    public void readFromNBT(NBTTagCompound nbtTags)
    {
        super.readFromNBT(nbtTags);

        isActive = nbtTags.getBoolean("isActive");
    }

	@Override
    public void writeToNBT(NBTTagCompound nbtTags)
    {
        super.writeToNBT(nbtTags);
        
        nbtTags.setBoolean("isActive", isActive);
    }
	
	@Override
	@SideOnly(Side.CLIENT)
	public AxisAlignedBB getRenderBoundingBox()
	{
		return INFINITE_EXTENT_AABB;
	}

	@Override
	public int fill(ForgeDirection from, LiquidStack resource, boolean doFill)
	{
		if(!isActive)
		{
			return getNetwork().emit(resource, doFill, Object3D.get(this).getFromSide(from).getTileEntity(worldObj));
		}
		
		return 0;
	}

	@Override
	public int fill(int tankIndex, LiquidStack resource, boolean doFill) 
	{
		if(!isActive)
		{
			return getNetwork().emit(resource, doFill, null);
		}
		
		return 0;
	}

	@Override
	public LiquidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) 
	{
		return null;
	}

	@Override
	public LiquidStack drain(int tankIndex, int maxDrain, boolean doDrain) 
	{
		return null;
	}

	@Override
	public ILiquidTank[] getTanks(ForgeDirection direction) 
	{
		return new ILiquidTank[] {dummyTank};
	}

	@Override
	public ILiquidTank getTank(ForgeDirection direction, LiquidStack type) 
	{
		return dummyTank;
	}
}
