package net.foundations.pl4.client;

import java.util.*;
import net.foundations.pl4.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server snapshots only: no client inventory mutation or remote coordinates in actions. */
public final class WirelessStorageScreen extends Screen {
    private record Row(int slot,String name,int count){}
    private final List<Row> rows=new ArrayList<>();
    private UUID token;private int page,slots,left,top,w;private String status="";private boolean closed,busy;
    public WirelessStorageScreen(CompoundTag tag){super(Component.literal("Wireless Storage"));read(tag);}
    private void read(CompoundTag tag){
        status=tag.getString("status");closed=tag.getBoolean("closed");busy=false;rows.clear();if(closed)return;
        token=UUID.fromString(tag.getString("token"));page=tag.getInt("page");slots=tag.getInt("slots");
        ListTag list=tag.getList("storageRows",Tag.TAG_COMPOUND);for(int i=0;i<Math.min(WirelessStorage.PAGE_SIZE,list.size());i++){CompoundTag row=list.getCompound(i);rows.add(new Row(row.getInt("slot"),row.getString("name"),row.getInt("count")));}
    }
    public void update(CompoundTag tag){read(tag);rebuildWidgets();}
    @Override protected void init(){
        w=Math.min(420,width-12);left=(width-w)/2;top=(height-224)/2;
        for(int i=0;i<rows.size();i++){Row row=rows.get(i);int y=top+43+i*23;button("1",left+w-88,y,32,()->send("withdraw",row.slot(),1)).active=!closed&&row.count()>0;button("64",left+w-52,y,40,()->send("withdraw",row.slot(),64)).active=!closed&&row.count()>0;}
        button("<",left+10,top+184,24,()->send("page",page-1,0)).active=!closed&&page>0;
        button(">",left+38,top+184,24,()->send("page",page+1,0)).active=!closed&&(page+1)*WirelessStorage.PAGE_SIZE<slots;
        button("Refresh",left+68,top+184,62,()->send("refresh",0,0));
        button("Deposit offhand",left+136,top+184,108,()->send("deposit",0,0));
        button("Done",left+w-62,top+184,50,this::onClose).active=true;
    }
    private Button button(String label,int x,int y,int width,Runnable action){Button b=Button.builder(Component.literal(label),unused->action.run()).bounds(x,y,width,20).build();b.active=!closed;addRenderableWidget(b);return b;}
    private void send(String action,int index,int amount){if(closed||busy)return;busy=true;PacketDistributor.sendToServer(new PLPackets.StorageRequest(token,action,index,amount));}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float partial){
        super.renderBackground(g,mx,my,partial);g.fill(left,top,left+w,top+224,0xF21B2533);g.fill(left,top,left+w,top+25,0xFF293D56);
        g.drawString(font,title,left+10,top+8,0xFFE4F3FF,false);
        g.drawString(font,"Page "+(page+1)+" / "+Math.max(1,(slots+WirelessStorage.PAGE_SIZE-1)/WirelessStorage.PAGE_SIZE)+" · "+slots+" slots",left+10,top+29,0xFF8CAEC5,false);
        for(int i=0;i<rows.size();i++){Row row=rows.get(i);int y=top+49+i*23;g.drawString(font,font.plainSubstrByWidth((row.slot()+1)+". "+row.name()+" × "+row.count(),w-108),left+10,y,0xFFDBEFFF,false);}
        g.drawString(font,font.plainSubstrByWidth(status.isEmpty()?"Main hand: tool · offhand: stack to deposit":status,w-20),left+10,top+210,0xFF8CAEC5,false);
    }
}
