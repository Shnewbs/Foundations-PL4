package net.foundations.pl4.client;

import java.util.*;
import net.foundations.pl4.*;
import net.foundations.pl4.compat.GuiGraphics;
import net.foundations.pl4.compat.Button;
import net.foundations.pl4.compat.EditBox;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.text.ITextComponent;
import net.foundations.pl4.compat.PacketDistributor;

/** Server snapshots only: no client inventory mutation or remote coordinates in actions. */
public final class WirelessStorageScreen extends net.foundations.pl4.compat.PortScreen {
    private record Row(int slot,String name,long count){}
    private final List<Row> rows=new ArrayList<>();
    private UUID token;private int page,slots,endpoints,left,top,w;private boolean limited;private String query="",sort="NAME";private EditBox search;private String status="";private boolean closed,busy;
    public WirelessStorageScreen(CompoundNBT tag){super(new net.minecraft.util.text.StringTextComponent("Wireless Storage"));read(tag);}
    private void read(CompoundNBT tag){
        status=tag.getString("status");closed=tag.getBoolean("closed");busy=false;rows.clear();if(closed)return;
        token=UUID.fromString(tag.getString("token"));page=tag.getInt("page");slots=tag.getInt("slots");endpoints=tag.getInt("endpoints");limited=tag.getBoolean("limited");query=tag.getString("query");sort=tag.getString("sort");
        ListNBT list=tag.getList("storageRows",net.minecraftforge.common.util.Constants.NBT.TAG_COMPOUND);for(int i=0;i<Math.min(WirelessStorage.PAGE_SIZE,list.size());i++){CompoundNBT row=list.getCompound(i);rows.add(new Row(row.getInt("slot"),row.getString("name"),row.getLong("count")));}
    }
    public void update(CompoundNBT tag){read(tag);rebuildWidgets();}
    @Override protected void init(){
        w=Math.min(420,width-12);left=(width-w)/2;top=(height-254)/2;
        search=new EditBox(font,left+10,top+43,w-174,20,new net.minecraft.util.text.StringTextComponent("Search items"));search.setMaxLength(64);search.setValue(query);addRenderableWidget(search);
        button("Search",left+w-158,top+43,62,()->send("search",0,0));
        button(sort.equals("COUNT")?"By count":"By name",left+w-90,top+43,80,()->{sort=sort.equals("NAME")?"COUNT":"NAME";send("search",0,0);});
        for(int i=0;i<rows.size();i++){Row row=rows.get(i);int y=top+73+i*23;button("1",left+w-88,y,32,()->send("withdraw",row.slot(),1)).active=!closed&&row.count()>0;button("64",left+w-52,y,40,()->send("withdraw",row.slot(),64)).active=!closed&&row.count()>0;}
        button("<",left+10,top+214,24,()->send("page",page-1,0)).active=!closed&&page>0;
        button(">",left+38,top+214,24,()->send("page",page+1,0)).active=!closed&&(page+1)*WirelessStorage.PAGE_SIZE<slots;
        button("Refresh",left+68,top+214,62,()->send("refresh",0,0));
        button("Deposit offhand",left+136,top+214,108,()->send("deposit",0,0));
        button("Done",left+w-62,top+214,50,this::onClose).active=true;
    }
    private Button button(String label,int x,int y,int width,Runnable action){Button b=Button.builder(new net.minecraft.util.text.StringTextComponent(label),unused->action.run()).bounds(x,y,width,20).build();b.active=!closed;addRenderableWidget(b);return b;}
    private void send(String action,int index,int amount){if(closed||busy)return;busy=true;PacketDistributor.sendToServer(new PLPackets.StorageRequest(token,action,index,amount,search.getValue(),sort));}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float partial){
        super.renderBackground(g,mx,my,partial);g.fill(left,top,left+w,top+254,0xF21B2533);g.fill(left,top,left+w,top+25,0xFF293D56);
        g.drawString(font,title,left+10,top+8,0xFFE4F3FF,false);
        g.drawString(font,"Page "+(page+1)+" / "+Math.max(1,(slots+WirelessStorage.PAGE_SIZE-1)/WirelessStorage.PAGE_SIZE)+" · "+slots+" variants · "+endpoints+" inventories",left+10,top+29,0xFF8CAEC5,false);
        for(int i=0;i<rows.size();i++){Row row=rows.get(i);int y=top+79+i*23;g.drawString(font,font.plainSubstrByWidth((row.slot()+1)+". "+row.name()+" × "+row.count(),w-108),left+10,y,0xFFDBEFFF,false);}
        g.drawString(font,font.plainSubstrByWidth(status.isEmpty()?(limited?"Scan limit reached · narrow the network":"Main hand: tool · offhand: stack to deposit"):status,w-20),left+10,top+240,0xFF8CAEC5,false);
    }
}
