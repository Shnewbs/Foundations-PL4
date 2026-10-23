package net.foundations.pl4.legacy;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
public final class LegacyWirelessGuiHandler implements net.minecraftforge.fml.common.network.IGuiHandler {
    public Object getServerGuiElement(int id,EntityPlayer player,World world,int x,int y,int z){return id==0&&LegacyWirelessStorage.resolve(player,player.inventory.getCurrentItem())!=null?new LegacyWirelessContainer(player,false):null;}
    @net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT) public Object getClientGuiElement(int id,EntityPlayer player,World world,int x,int y,int z){return id==0?new LegacyWirelessScreen(new LegacyWirelessContainer(player,true)):null;}
}
