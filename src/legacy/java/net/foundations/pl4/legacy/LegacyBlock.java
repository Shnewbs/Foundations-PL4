package net.foundations.pl4.legacy;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
/** Full-block legacy hosts, with IDs separate from modern multipart saves. */
public final class LegacyBlock extends BlockContainer {
    public final int role;
    public LegacyBlock(String name,int role){super(Material.IRON);this.role=role;setRegistryName(LegacyPL4.ID,name);setUnlocalizedName(LegacyPL4.ID+"."+name);setHardness(2F);setResistance(6000000F);setCreativeTab(CreativeTabs.REDSTONE);}
    @Override public TileEntity createNewTileEntity(World world,int metadata){return new LegacyTile();}
    @Override public EnumBlockRenderType getRenderType(IBlockState state){return EnumBlockRenderType.MODEL;}
    @Override public void onBlockPlacedBy(World world,BlockPos pos,IBlockState state,EntityLivingBase entity,ItemStack stack){
        if(world.isRemote||!(world.getTileEntity(pos) instanceof LegacyTile))return;
        LegacyTile tile=(LegacyTile)world.getTileEntity(pos);tile.owner=entity.getUniqueID().toString();tile.side=entity.getHorizontalFacing().getOpposite().getIndex();tile.markDirty();
    }
    @Override public boolean onBlockActivated(World world,BlockPos pos,IBlockState state,EntityPlayer player,EnumHand hand,EnumFacing face,float x,float y,float z){
        if(world.isRemote)return true;TileEntity raw=world.getTileEntity(pos);if(!(raw instanceof LegacyTile))return false;LegacyTile tile=(LegacyTile)raw;
        if(!tile.canEdit(player)){player.sendMessage(new TextComponentString("PL4: This host belongs to another player."));return true;}
        if(LegacyFluidPlatform.interact(tile,player,player.getHeldItem(hand))){player.sendMessage(new TextComponentString(tile.describe()));return true;}
        if(player.isSneaking()&&role!=0&&role!=5){tile.side=face.getIndex();tile.markDirty();}
        player.sendMessage(new TextComponentString(tile.describe()));
        player.sendMessage(new TextComponentString("Sneak-click a node face to choose its adjacent inventory. /pl4legacy recipes lists registered recipes."));return true;
    }
    @Override public void breakBlock(World world,BlockPos pos,IBlockState state){
        if(!world.isRemote&&world.getTileEntity(pos) instanceof LegacyTile){LegacyTile tile=(LegacyTile)world.getTileEntity(pos);LegacyFluidPlatform.drop(tile);if(!tile.pending.isEmpty()){ItemStack held=tile.pending;tile.pending=ItemStack.EMPTY;tile.markDirty();world.spawnEntity(new EntityItem(world,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,held));}}
        super.breakBlock(world,pos,state);
    }
    @Override public net.minecraft.block.material.EnumPushReaction getMobilityFlag(IBlockState state){return net.minecraft.block.material.EnumPushReaction.BLOCK;}
}
