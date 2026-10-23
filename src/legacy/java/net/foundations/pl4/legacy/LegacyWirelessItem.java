package net.foundations.pl4.legacy;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
/** Bind by sneak-clicking an owned host, then use within 64 blocks in the same dimension. */
public final class LegacyWirelessItem extends Item {
    public LegacyWirelessItem(int id){super(id);setUnlocalizedName("foundations_pl4.legacy_wireless_storage");setTextureName("compass");setMaxStackSize(1);setCreativeTab(net.minecraft.creativetab.CreativeTabs.tabRedstone);}
    @Override public ItemStack onItemRightClick(ItemStack stack,World world,EntityPlayer player){if(!world.isRemote){if(LegacyWirelessStorage.resolve(player,stack)!=null)player.openGui(LegacyPL4.instance,0,world,0,0,0);else player.addChatMessage("PL4: Sneak-click an owned host to bind; stay within 64 blocks in this dimension.");}return stack;}
    @Override public boolean onItemUse(ItemStack stack,EntityPlayer player,World world,int x,int y,int z,int face,float hitX,float hitY,float hitZ){if(!world.isRemote&&player.isSneaking()){net.minecraft.tileentity.TileEntity raw=world.getBlockTileEntity(x,y,z);if(raw instanceof LegacyTile&&LegacyWirelessStorage.bind(player,stack,(LegacyTile)raw))player.addChatMessage("PL4: Wireless Storage bound. Use to open; click inventory items to deposit.");}else onItemRightClick(stack,world,player);return true;}
}
