package mekanism.common;

import ic2.api.item.ICustomElectricItem;

import java.util.List;

import mekanism.api.EnumColor;
import mekanism.api.IEnergizedItem;
import mekanism.api.IUpgradeManagement;
import mekanism.common.BlockMachine.MachineType;
import mekanism.common.PacketHandler.Transmission;
import mekanism.common.network.PacketElectricChest;
import mekanism.common.network.PacketElectricChest.ElectricChestPacketType;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.liquids.LiquidDictionary;
import net.minecraftforge.liquids.LiquidStack;

import org.lwjgl.input.Keyboard;

import thermalexpansion.api.item.IChargeableItem;
import universalelectricity.core.electricity.ElectricityDisplay;
import universalelectricity.core.electricity.ElectricityDisplay.ElectricUnit;
import universalelectricity.core.electricity.ElectricityPack;
import universalelectricity.core.item.IItemElectric;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Item class for handling multiple machine block IDs.
 * 0: Enrichment Chamber
 * 1: Osmium Compressor
 * 2: Combiner
 * 3: Crusher
 * 4: Theoretical Elementizer
 * 5: Basic Factory
 * 6: Advanced Factory
 * 7: Elite Factory
 * 8: Metallurgic Infuser
 * 9: Purification Chamber
 * 10: Energized Smelter
 * 11: Teleporter
 * 12: Electric Pump
 * 13: Electric Chest
 * 14: Chargepad
 * @author AidanBrady
 *
 */
public class ItemBlockMachine extends ItemBlock implements IEnergizedItem, IItemElectric, ICustomElectricItem, IUpgradeManagement, IFactory, ISustainedInventory, ISustainedTank, IElectricChest, IChargeableItem
{
	public Block metaBlock;
	
	public ItemBlockMachine(int id, Block block)
	{
		super(id);
		metaBlock = block;
		setHasSubtypes(true);
		setNoRepair();
		setMaxStackSize(1);
	}
	
	@Override
	public int getMetadata(int i)
	{
		return i;
	}
	
	@Override
	public String getUnlocalizedName(ItemStack itemstack)
	{
		String name = "";
		switch(itemstack.getItemDamage())
		{
			case 0:
				name = "EnrichmentChamber";
				break;
			case 1:
				name = "OsmiumCompressor";
				break;
			case 2:
				name = "Combiner";
				break;
			case 3:
				name = "Crusher";
				break;
			case 4:
				name = "TheoreticalElementizer";
				break;
			case 5:
				name = "BasicFactory";
				break;
			case 6:
				name = "AdvancedFactory";
				break;
			case 7:
				name = "EliteFactory";
				break;
			case 8:
				name = "MetallurgicInfuser";
				break;
			case 9:
				name = "PurificationChamber";
				break;
			case 10:
				name = "EnergizedSmelter";
				break;
			case 11:
				name = "Teleporter";
				break;
			case 12:
				name = "ElectricPump";
				break;
			case 13:
				name = "ElectricChest";
				break;
			case 14:
				name = "Chargepad";
				break;
			default:
				name = "Unknown";
				break;
		}
		return getUnlocalizedName() + "." + name;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack itemstack, EntityPlayer entityplayer, List list, boolean flag)
	{
		if(!Keyboard.isKeyDown(Keyboard.KEY_LSHIFT))
		{
			list.add("Hold " + EnumColor.AQUA + "shift" + EnumColor.GREY + " for more details.");
		}
		else {
			if(isFactory(itemstack))
			{
				list.add(EnumColor.INDIGO + "Recipe Type: " + EnumColor.GREY + RecipeType.values()[getRecipeType(itemstack)].getName());
			}
			
			if(isElectricChest(itemstack))
			{
				list.add(EnumColor.INDIGO + "Authenticated: " + EnumColor.GREY + getAuthenticated(itemstack));
				list.add(EnumColor.INDIGO + "Locked: " + EnumColor.GREY + getLocked(itemstack));
			}
			
			list.add(EnumColor.BRIGHT_GREEN + "Stored Energy: " + EnumColor.GREY + ElectricityDisplay.getDisplayShort(getJoules(itemstack), ElectricUnit.JOULES));
			list.add(EnumColor.BRIGHT_GREEN + "Voltage: " + EnumColor.GREY + getVoltage(itemstack) + "v");
			
			if(hasTank(itemstack))
			{
				if(getLiquidStack(itemstack) != null)
				{
					list.add(EnumColor.PINK + LiquidDictionary.findLiquidName(getLiquidStack(itemstack)) + ": " + EnumColor.GREY + getLiquidStack(itemstack).amount + "mB");
				}
			}
			
			if(supportsUpgrades(itemstack))
			{
				list.add(EnumColor.PURPLE + "Energy: " + EnumColor.GREY + "x" + (getEnergyMultiplier(itemstack)+1));
				list.add(EnumColor.PURPLE + "Speed: " + EnumColor.GREY + "x" + (getSpeedMultiplier(itemstack)+1));
			}
			
			if(itemstack.getItemDamage() != 14)
			{
				list.add(EnumColor.AQUA + "Inventory: " + EnumColor.GREY + (getInventory(itemstack) != null && getInventory(itemstack).tagCount() != 0));
			}
		}
	}

	@Override
	public double getJoules(ItemStack itemStack)
	{
		return getEnergy(itemStack);
	}

	@Override
	public void setJoules(double wattHours, ItemStack itemStack)
	{
		setEnergy(itemStack, wattHours);
	}

	@Override
	public double getMaxJoules(ItemStack itemStack)
	{
		return getMaxEnergy(itemStack);
	}

	@Override
	public double getVoltage(ItemStack itemStack) 
	{
		return 120;
	}

	@Override
	public ElectricityPack onReceive(ElectricityPack electricityPack, ItemStack itemStack)
	{
		double rejectedElectricity = Math.max((getJoules(itemStack) + electricityPack.getWatts()) - getMaxJoules(itemStack), 0);
		double joulesToStore = electricityPack.getWatts() - rejectedElectricity;
		this.setJoules(getJoules(itemStack) + joulesToStore, itemStack);
		return ElectricityPack.getFromWatts(joulesToStore, getVoltage(itemStack));
	}

	@Override
	public ElectricityPack onProvide(ElectricityPack electricityPack, ItemStack itemStack)
	{
		return new ElectricityPack();
	}

	@Override
	public ElectricityPack getReceiveRequest(ItemStack itemStack)
	{
		return ElectricityPack.getFromWatts(Math.min(getMaxJoules(itemStack) - getJoules(itemStack), getTransferRate(itemStack)), getVoltage(itemStack));
	}

	@Override
	public ElectricityPack getProvideRequest(ItemStack itemStack)
	{
		return new ElectricityPack();
	}
	
	public double getTransferRate(ItemStack itemStack)
	{
		return getMaxTransfer(itemStack);
	}
	
	@Override
	public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int metadata)
    {
    	boolean place = super.placeBlockAt(stack, player, world, x, y, z, side, hitX, hitY, hitZ, metadata);
    	
    	if(place)
    	{
    		TileEntityElectricBlock tileEntity = (TileEntityElectricBlock)world.getBlockTileEntity(x, y, z);
    		
    		if(tileEntity instanceof IUpgradeManagement)
    		{
    			((IUpgradeManagement)tileEntity).setEnergyMultiplier(getEnergyMultiplier(stack));
    			((IUpgradeManagement)tileEntity).setSpeedMultiplier(getSpeedMultiplier(stack));
    		}
    		
    		if(tileEntity instanceof TileEntityFactory)
    		{
    			((TileEntityFactory)tileEntity).recipeType = getRecipeType(stack);
    		}
    		
    		if(tileEntity instanceof ISustainedTank)
    		{
    			if(hasTank(stack) && getLiquidStack(stack) != null)
    			{
    				((ISustainedTank)tileEntity).setLiquidStack(getLiquidStack(stack));
    			}
    		}
    		
    		if(tileEntity instanceof TileEntityElectricChest)
    		{
    			((TileEntityElectricChest)tileEntity).authenticated = getAuthenticated(stack);
    			((TileEntityElectricChest)tileEntity).locked = getLocked(stack);
    			((TileEntityElectricChest)tileEntity).password = getPassword(stack);
    		}
    		
    		((ISustainedInventory)tileEntity).setInventory(getInventory(stack));
    		
    		tileEntity.electricityStored = getJoules(stack);
    	}
    	
    	return place;
    }
	
	@Override
	public int charge(ItemStack itemStack, int amount, int tier, boolean ignoreTransferLimit, boolean simulate)
	{
		double energyNeeded = getMaxEnergy(itemStack)-getEnergy(itemStack);
		double energyToStore = Math.min(Math.min(amount*Mekanism.FROM_IC2, getMaxEnergy(itemStack)*0.01), energyNeeded);
		
		if(!simulate)
		{
			setEnergy(itemStack, getEnergy(itemStack) + energyToStore);
		}
		
		return (int)(energyToStore*Mekanism.TO_IC2);
	}
	
	@Override
	public int discharge(ItemStack itemStack, int amount, int tier, boolean ignoreTransferLimit, boolean simulate)
	{
		return 0;
	}

	@Override
	public boolean canUse(ItemStack itemStack, int amount)
	{
		return getJoules(itemStack) >= amount*Mekanism.FROM_IC2;
	}
	
	@Override
	public boolean canShowChargeToolTip(ItemStack itemStack)
	{
		return false;
	}
	
	@Override
	public boolean canProvideEnergy(ItemStack itemStack)
	{
		return false;
	}

	@Override
	public int getChargedItemId(ItemStack itemStack)
	{
		return itemID;
	}

	@Override
	public int getEmptyItemId(ItemStack itemStack)
	{
		return itemID;
	}

	@Override
	public int getMaxCharge(ItemStack itemStack)
	{
		return 0;
	}

	@Override
	public int getTier(ItemStack itemStack)
	{
		return 3;
	}

	@Override
	public int getTransferLimit(ItemStack itemStack)
	{
		return 0;
	}
	
	@Override
	public void onUpdate(ItemStack itemstack, World world, Entity entity, int i, boolean flag)
	{
		if(isElectricChest(itemstack))
		{
			setPrevLidAngle(itemstack, getLidAngle(itemstack));
			float increment = 0.1F;
			
		    if((!getOpen(itemstack) && getLidAngle(itemstack) > 0.0F) || (getOpen(itemstack) && getLidAngle(itemstack) < 1.0F))
		    {
		    	float angle = getLidAngle(itemstack);
	
		    	if(getOpen(itemstack))
		    	{
		    		setLidAngle(itemstack, getLidAngle(itemstack)+increment);
		    	}
		    	else {
		    		setLidAngle(itemstack, getLidAngle(itemstack)-increment);
		    	}
	
		    	if(getLidAngle(itemstack) > 1.0F)
		    	{
		    		setLidAngle(itemstack, 1.0F);
		    	}
	
		     	float split = 0.5F;
	
		     	if(getLidAngle(itemstack) < 0.0F)
		     	{
		     		setLidAngle(itemstack, 0.0F);
		     	}
		    }
		}
	}
	
	@Override
	public boolean onEntityItemUpdate(EntityItem entityItem)
	{
		 onUpdate(entityItem.getEntityItem(), null, entityItem, 0, false);
		 return false;
	}

	@Override
	public int getEnergyMultiplier(Object... data) 
	{
		if(data[0] instanceof ItemStack)
		{
			ItemStack itemStack = (ItemStack) data[0];

			if(itemStack.stackTagCompound == null) 
			{ 
				return 0; 
			}
			
			return itemStack.stackTagCompound.getInteger("energyMultiplier");
		}

		return 0;
	}

	@Override
	public void setEnergyMultiplier(int multiplier, Object... data) 
	{
		if(data[0] instanceof ItemStack)
		{
			ItemStack itemStack = (ItemStack)data[0];

			if(itemStack.stackTagCompound == null)
			{
				itemStack.setTagCompound(new NBTTagCompound());
			}

			itemStack.stackTagCompound.setInteger("energyMultiplier", multiplier);
		}
	}

	@Override
	public int getSpeedMultiplier(Object... data) 
	{
		if(data[0] instanceof ItemStack)
		{
			ItemStack itemStack = (ItemStack) data[0];

			if(itemStack.stackTagCompound == null) 
			{ 
				return 0; 
			}
			
			return itemStack.stackTagCompound.getInteger("speedMultiplier");
		}

		return 0;
	}

	@Override
	public void setSpeedMultiplier(int multiplier, Object... data) 
	{
		if(data[0] instanceof ItemStack)
		{
			ItemStack itemStack = (ItemStack)data[0];

			if(itemStack.stackTagCompound == null)
			{
				itemStack.setTagCompound(new NBTTagCompound());
			}

			itemStack.stackTagCompound.setInteger("speedMultiplier", multiplier);
		}
	}
	
	@Override
	public boolean supportsUpgrades(Object... data)
	{
		if(data[0] instanceof ItemStack)
		{
			if(((ItemStack)data[0]).getItemDamage() != 11 && ((ItemStack)data[0]).getItemDamage() != 12 && ((ItemStack)data[0]).getItemDamage() != 13 && ((ItemStack)data[0]).getItemDamage() != 14)
			{
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public ItemStack onItemRightClick(ItemStack itemstack, World world, EntityPlayer entityplayer)
	{
		if(!world.isRemote)
		{
			if(isElectricChest(itemstack))
			{
		 		if(!getAuthenticated(itemstack))
		 		{
		 			PacketHandler.sendPacket(Transmission.SINGLE_CLIENT, new PacketElectricChest().setParams(ElectricChestPacketType.CLIENT_OPEN, 2, 0, false), entityplayer);
		 		}
		 		else if(getLocked(itemstack) && getJoules(itemstack) > 0)
		 		{
		 			PacketHandler.sendPacket(Transmission.SINGLE_CLIENT, new PacketElectricChest().setParams(ElectricChestPacketType.CLIENT_OPEN, 1, 0, false), entityplayer);
		 		}
		 		else {
		 			InventoryElectricChest inventory = new InventoryElectricChest(entityplayer);
		 			MekanismUtils.openElectricChestGui((EntityPlayerMP)entityplayer, null, inventory, false);
		 		}
			}
		}
		
		return itemstack;
	}

	@Override
	public int getRecipeType(ItemStack itemStack)
	{
		if(itemStack.stackTagCompound == null) 
		{ 
			return 0; 
		}
		
		return itemStack.stackTagCompound.getInteger("recipeType");
	}

	@Override
	public void setRecipeType(int type, ItemStack itemStack) 
	{
		if(itemStack.stackTagCompound == null)
		{
			itemStack.setTagCompound(new NBTTagCompound());
		}

		itemStack.stackTagCompound.setInteger("recipeType", type);
	}
	
	@Override
	public boolean isFactory(ItemStack itemStack)
	{
		return itemStack.getItem() instanceof ItemBlockMachine && itemStack.getItemDamage() >= 5 && itemStack.getItemDamage() <= 7;
	}

	@Override
	public void setInventory(NBTTagList nbtTags, Object... data) 
	{
		if(data[0] instanceof ItemStack)
		{
			ItemStack itemStack = (ItemStack)data[0];
			
			if(itemStack.stackTagCompound == null)
			{
				itemStack.setTagCompound(new NBTTagCompound());
			}
	
			itemStack.stackTagCompound.setTag("Items", nbtTags);
		}
	}

	@Override
	public NBTTagList getInventory(Object... data) 
	{
		if(data[0] instanceof ItemStack)
		{
			ItemStack itemStack = (ItemStack)data[0];
			
			if(itemStack.stackTagCompound == null) 
			{ 
				return null; 
			}
			
			return itemStack.stackTagCompound.getTagList("Items");
		}
		
		return null;
	}

	@Override
	public void setLiquidStack(LiquidStack liquidStack, Object... data) 
	{
		if(liquidStack == null || liquidStack.amount == 0 || liquidStack.itemID == 0)
		{
			return;
		}
		
		if(data[0] instanceof ItemStack)
		{
			ItemStack itemStack = (ItemStack)data[0];
			
			if(itemStack.stackTagCompound == null)
			{
				itemStack.setTagCompound(new NBTTagCompound());
			}
			
			itemStack.stackTagCompound.setTag("liquidTank", liquidStack.writeToNBT(new NBTTagCompound()));
		}
	}

	@Override
	public LiquidStack getLiquidStack(Object... data) 
	{
		if(data[0] instanceof ItemStack)
		{
			ItemStack itemStack = (ItemStack)data[0];
			
			if(itemStack.stackTagCompound == null) 
			{ 
				return null; 
			}
			
			if(itemStack.stackTagCompound.hasKey("liquidTank"))
			{
				return LiquidStack.loadLiquidStackFromNBT(itemStack.stackTagCompound.getCompoundTag("liquidTank"));
			}
		}
		
		return null;
	}

	@Override
	public boolean hasTank(Object... data) 
	{
		return data[0] instanceof ItemStack && ((ItemStack)data[0]).getItem() instanceof ISustainedTank && ((ItemStack)data[0]).getItemDamage() == 12;
	}

	@Override
	public void setAuthenticated(ItemStack itemStack, boolean auth) 
	{
		if(itemStack.stackTagCompound == null)
		{
			itemStack.setTagCompound(new NBTTagCompound());
		}

		itemStack.stackTagCompound.setBoolean("authenticated", auth);
	}

	@Override
	public boolean getAuthenticated(ItemStack itemStack) 
	{
		if(itemStack.stackTagCompound == null) 
		{ 
			return false; 
		}
		
		return itemStack.stackTagCompound.getBoolean("authenticated");
	}

	@Override
	public boolean isElectricChest(ItemStack itemStack) 
	{
		return itemStack.getItemDamage() == 13;
	}

	@Override
	public void setPassword(ItemStack itemStack, String pass)
	{
		if(itemStack.stackTagCompound == null)
		{
			itemStack.setTagCompound(new NBTTagCompound());
		}

		itemStack.stackTagCompound.setString("password", pass);
	}

	@Override
	public String getPassword(ItemStack itemStack)
	{
		if(itemStack.stackTagCompound == null) 
		{ 
			return "";
		}
		
		return itemStack.stackTagCompound.getString("password");
	}

	@Override
	public void setLocked(ItemStack itemStack, boolean locked) 
	{
		if(itemStack.stackTagCompound == null)
		{
			itemStack.setTagCompound(new NBTTagCompound());
		}

		itemStack.stackTagCompound.setBoolean("locked", locked);
	}

	@Override
	public boolean getLocked(ItemStack itemStack) 
	{
		if(itemStack.stackTagCompound == null) 
		{ 
			return false; 
		}
		
		return itemStack.stackTagCompound.getBoolean("locked");
	}
	
	@Override
	public void setOpen(ItemStack itemStack, boolean open) 
	{
		if(itemStack.stackTagCompound == null)
		{
			itemStack.setTagCompound(new NBTTagCompound());
		}

		itemStack.stackTagCompound.setBoolean("open", open);
	}

	@Override
	public boolean getOpen(ItemStack itemStack) 
	{
		if(itemStack.stackTagCompound == null) 
		{ 
			return false; 
		}
		
		return itemStack.stackTagCompound.getBoolean("open");
	}
	
	@Override
	public void setLidAngle(ItemStack itemStack, float lidAngle) 
	{
		if(itemStack.stackTagCompound == null)
		{
			itemStack.setTagCompound(new NBTTagCompound());
		}

		itemStack.stackTagCompound.setFloat("lidAngle", lidAngle);
	}

	@Override
	public float getLidAngle(ItemStack itemStack) 
	{
		if(itemStack.stackTagCompound == null) 
		{ 
			return 0.0F; 
		}
		
		return itemStack.stackTagCompound.getFloat("lidAngle");
	}
	
	@Override
	public void setPrevLidAngle(ItemStack itemStack, float prevLidAngle) 
	{
		if(itemStack.stackTagCompound == null)
		{
			itemStack.setTagCompound(new NBTTagCompound());
		}

		itemStack.stackTagCompound.setFloat("prevLidAngle", prevLidAngle);
	}

	@Override
	public float getPrevLidAngle(ItemStack itemStack) 
	{
		if(itemStack.stackTagCompound == null) 
		{ 
			return 0.0F; 
		}
		
		return itemStack.stackTagCompound.getFloat("prevLidAngle");
	}
	
	@Override
	public double getEnergy(ItemStack itemStack) 
	{
		if(itemStack.stackTagCompound == null) 
		{ 
			return 0; 
		}
		
		return itemStack.stackTagCompound.getDouble("electricity");
	}

	@Override
	public void setEnergy(ItemStack itemStack, double amount) 
	{
		if(itemStack.stackTagCompound == null)
		{
			itemStack.setTagCompound(new NBTTagCompound());
		}

		double electricityStored = Math.max(Math.min(amount, getMaxJoules(itemStack)), 0);
		itemStack.stackTagCompound.setDouble("electricity", electricityStored);
	}

	@Override
	public double getMaxEnergy(ItemStack itemStack) 
	{
		return MekanismUtils.getEnergy(getEnergyMultiplier(itemStack), MachineType.getFromMetadata(itemStack.getItemDamage()).baseEnergy);
	}

	@Override
	public double getMaxTransfer(ItemStack itemStack) 
	{
		return getMaxEnergy(itemStack)*0.005;
	}

	@Override
	public boolean canReceive(ItemStack itemStack) 
	{
		return true;
	}

	@Override
	public boolean canSend(ItemStack itemStack)
	{
		return false;
	}
	
	@Override
	public float receiveEnergy(ItemStack theItem, float energy, boolean doReceive)
	{
		double energyNeeded = getMaxEnergy(theItem)-getEnergy(theItem);
		double toReceive = Math.min(energy*Mekanism.FROM_BC, energyNeeded);
		
		if(doReceive)
		{
			setEnergy(theItem, getEnergy(theItem) + toReceive);
		}
		
		return (float)(toReceive*Mekanism.TO_BC);
	}

	@Override
	public float transferEnergy(ItemStack theItem, float energy, boolean doTransfer) 
	{
		return 0;
	}

	@Override
	public float getEnergyStored(ItemStack theItem)
	{
		return (float)(getEnergy(theItem)*Mekanism.TO_BC);
	}

	@Override
	public float getMaxEnergyStored(ItemStack theItem)
	{
		return (float)(getMaxEnergy(theItem)*Mekanism.TO_BC);
	}
	
	@Override
	public boolean isMetadataSpecific()
	{
		return true;
	}
}
