package net.foundations.pl4.legacy;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
public final class LegacyBlock extends BlockContainer {
    public final int role;
    public LegacyBlock(int id,String name,int role){super(id,Material.iron);this.role=role;setUnlocalizedName(LegacyPL4.ID+"."+name);setTextureName(LegacyPL4.ID+":"+name);setHardness(2F);setResistance(6000000F);setCreativeTab(CreativeTabs.tabRedstone);}
    @Override public TileEntity createNewTileEntity(World world){return new LegacyTile();}
    @Override public void onBlockPlacedBy(World world,int x,int y,int z,EntityLivingBase entity,ItemStack stack){if(world.isRemote||!(world.getBlockTileEntity(x,y,z) instanceof LegacyTile))return;LegacyTile tile=(LegacyTile)world.getBlockTileEntity(x,y,z);tile.owner=entity instanceof EntityPlayer?LegacyTile.identity((EntityPlayer)entity):"";tile.side=2;tile.onInventoryChanged();}
    @Override public boolean onBlockActivated(World world,int x,int y,int z,EntityPlayer player,int side,float hitX,float hitY,float hitZ){
        if(world.isRemote)return true;TileEntity raw=world.getBlockTileEntity(x,y,z);if(!(raw instanceof LegacyTile))return false;LegacyTile tile=(LegacyTile)raw;
        if(!tile.canEdit(player)){player.addChatMessage("PL4: This host belongs to another player.");return true;}
        if(LegacyEnergyPlatform.interact(tile,player,player.getCurrentEquippedItem())){player.addChatMessage(tile.describe()+" | Sneak-click empty-handed to crank; 10-tick cooldown.");return true;}
        if(LegacyFluidPlatform.interact(tile,player,player.getCurrentEquippedItem())){player.addChatMessage(tile.describe());return true;}
        if(player.isSneaking()&&role!=0&&role!=5&&role!=8&&side>=0&&side<6){tile.side=side;tile.onInventoryChanged();}player.addChatMessage(tile.describe());player.addChatMessage("Sneak-click a node face to select adjacent inventory; /pl4legacy recipes lists registered recipes.");return true;
    }
    @Override public void breakBlock(World world,int x,int y,int z,int oldId,int oldMetadata){if(!world.isRemote&&world.getBlockTileEntity(x,y,z) instanceof LegacyTile){LegacyTile tile=(LegacyTile)world.getBlockTileEntity(x,y,z);LegacyEnergyPlatform.drop(tile);LegacyFluidPlatform.drop(tile);if(tile.pending!=null){ItemStack held=tile.pending;tile.pending=null;tile.onInventoryChanged();world.spawnEntityInWorld(new EntityItem(world,x+.5,y+.5,z+.5,held));}}super.breakBlock(world,x,y,z,oldId,oldMetadata);}
    @Override public int getMobilityFlag(){return 2;}
}
