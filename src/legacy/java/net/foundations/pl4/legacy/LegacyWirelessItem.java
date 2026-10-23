package net.foundations.pl4.legacy;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
/** Bind by sneak-clicking an owned host, then use within 64 blocks in the same dimension. */
public final class LegacyWirelessItem extends Item {
    public LegacyWirelessItem(){setRegistryName(LegacyPL4.ID,"legacy_wireless_storage");setUnlocalizedName(LegacyPL4.ID+".legacy_wireless_storage");setMaxStackSize(1);setCreativeTab(net.minecraft.creativetab.CreativeTabs.REDSTONE);}
    @Override public net.minecraft.util.ActionResult<ItemStack> onItemRightClick(World world,EntityPlayer player,net.minecraft.util.EnumHand hand){ItemStack stack=player.getHeldItem(hand);if(hand==net.minecraft.util.EnumHand.MAIN_HAND&&!world.isRemote){if(LegacyWirelessStorage.resolve(player,stack)!=null)player.openGui(LegacyPL4.instance,0,world,0,0,0);else player.sendMessage(new net.minecraft.util.text.TextComponentString("PL4: Sneak-click an owned host to bind; stay within 64 blocks in this dimension."));}return new net.minecraft.util.ActionResult<ItemStack>(net.minecraft.util.EnumActionResult.SUCCESS,stack);}
    @Override public net.minecraft.util.EnumActionResult onItemUse(EntityPlayer player,World world,net.minecraft.util.math.BlockPos pos,net.minecraft.util.EnumHand hand,net.minecraft.util.EnumFacing face,float x,float y,float z){if(hand!=net.minecraft.util.EnumHand.MAIN_HAND)return net.minecraft.util.EnumActionResult.PASS;if(!world.isRemote&&player.isSneaking()){net.minecraft.tileentity.TileEntity raw=world.getTileEntity(pos);if(raw instanceof LegacyTile&&LegacyWirelessStorage.bind(player,player.getHeldItem(hand),(LegacyTile)raw))player.sendMessage(new net.minecraft.util.text.TextComponentString("PL4: Wireless Storage bound. Use to open; click inventory items to deposit."));}else onItemRightClick(world,player,hand);return net.minecraft.util.EnumActionResult.SUCCESS;}
}
