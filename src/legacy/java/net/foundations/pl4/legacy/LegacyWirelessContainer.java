package net.foundations.pl4.legacy;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
/** Native window packets; virtual storage slots never permit vanilla pickup, drag, throw or hotbar swaps. */
public final class LegacyWirelessContainer extends Container {
    private final EntityPlayer owner;private final int toolSlot;private final ItemStack binding;private final boolean client;
    private final InventoryBasic view=new InventoryBasic("Wireless Storage",false,45);
    private List<LegacyWirelessStorage.Row> entries=new ArrayList<LegacyWirelessStorage.Row>();
    public static final int SORT_CONTROL=81,SEARCH_CLEAR=82,SEARCH_BACKSPACE=83,SEARCH_CHARACTER=84,SEARCH_LAST=178,GROUP_CONTROL=179;
    private boolean grouped;private int visibleRows;private final long[] totals=new long[45];
    public static int storageX(int slot){return 8+(slot%9)*18;}
    public static int storageY(int slot){return 18+(slot/9)*18;}
    public boolean grouped(){return grouped;}
    public long rowTotal(int slot){if(slot<0||slot>=45)return 0;long value=totals[slot];return value>=0&&value<=LegacyWirelessStorage.MAX_TOTAL?value:0;}
    private String search="";private int sentSearchRevision=-2,sentSearchListeners=-1;
    public String search(){return search;}
    public int page=0,revision=-1;private int sortOrder;
    public int sortOrder(){return sortOrder;}private long lastAction=Long.MIN_VALUE,lastRefresh=Long.MIN_VALUE;private boolean failed;
    public LegacyWirelessContainer(EntityPlayer player,boolean client){
        this.owner=player;this.client=client;this.toolSlot=player.inventory.currentItem;ItemStack held=player.inventory.getStackInSlot(toolSlot);this.binding=LegacyTile.STACKS.empty(held)?ItemStack.EMPTY:held.copy();
        for(int i=0;i<45;i++)addSlotToContainer(locked(view,i,storageX(i),storageY(i)));
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlotToContainer(locked(player.inventory,col+row*9+9,8+col*18,122+row*18));
        for(int i=0;i<9;i++)addSlotToContainer(locked(player.inventory,i,8+i*18,180));
        if(!client)refresh(false);
    }
    private static Slot locked(net.minecraft.inventory.IInventory inventory,int i,int x,int y){return new Slot(inventory,i,x,y){public boolean isItemValid(ItemStack stack){return false;}public boolean canTakeStack(EntityPlayer player){return false;}};}
    private ItemStack held(){return owner.inventory.getStackInSlot(toolSlot);}
    @Override public boolean canInteractWith(EntityPlayer player){return player==owner&&!failed&&(client||owner.inventory.currentItem==toolSlot&&LegacyWirelessStorage.tool(held())&&ItemStack.areItemStackTagsEqual(binding,held())&&LegacyWirelessStorage.resolve(owner,held())!=null);}
    private void refresh(boolean force){
        List<LegacyWirelessStorage.Row> next=LegacyWirelessStorage.rows(owner,held(),sortOrder,search,grouped);boolean changed=force||next.size()!=entries.size();
        if(!changed)for(int i=0;i<next.size();i++){LegacyWirelessStorage.Row a=entries.get(i),b=next.get(i);if(!LegacyWirelessStorage.sameRow(a,b)){changed=true;break;}}
        if(revision<0||changed){if(revision>=127){failed=true;return;}revision++;}
        entries=next;page=Math.max(0,Math.min(page,Math.max(0,(entries.size()-1)/45)));
        visibleRows=Math.min(45,Math.max(0,entries.size()-page*45));
        for(int i=0;i<45;i++){int at=page*45+i;totals[i]=at<entries.size()?entries.get(at).total:0;view.setInventorySlotContents(i,at<entries.size()?entries.get(at).preview():ItemStack.EMPTY);}
    }
    private void sendRevision(){
        boolean sendSearch=sentSearchRevision!=revision||sentSearchListeners!=listeners.size();
        for(Object listener:listeners){net.minecraft.inventory.IContainerListener sink=(net.minecraft.inventory.IContainerListener)listener;
            sink.sendWindowProperty(this,1,sortOrder);sink.sendWindowProperty(this,2,page);sink.sendWindowProperty(this,36,grouped?1:0);
            if(sendSearch){sink.sendWindowProperty(this,3,search.length());for(int i=0;i<search.length();i++)sink.sendWindowProperty(this,4+i,search.charAt(i));}
            if(sendSearch){sink.sendWindowProperty(this,37,grouped?visibleRows:0);if(grouped)for(int i=0;i<visibleRows;i++)for(int part=0;part<3;part++)sink.sendWindowProperty(this,64+i*3+part,(int)(totals[i]>>>(part*16))&65535);}
            // Revision is the final acknowledgement after the complete authoritative search.
            sink.sendWindowProperty(this,0,revision);
        }sentSearchRevision=revision;sentSearchListeners=listeners.size();
    }
    @Override public void updateProgressBar(int id,int value){
        if(id==0)revision=value;else if(id==1)sortOrder=Math.max(0,Math.min(2,value));else if(id==2)page=Math.max(0,value);
        else if(id==36&&(value==0||value==1))grouped=value==1;
        else if(id==37&&value>=0&&value<=45){visibleRows=value;java.util.Arrays.fill(totals,0);}
        else if(id>=64&&id<64+visibleRows*3){int slot=(id-64)/3,shift=((id-64)%3)*16;long mask=65535L<<shift;totals[slot]=(totals[slot]&~mask)|((value&65535L)<<shift);}
        else if(id==3&&value>=0&&value<=LegacyWirelessStorage.MAX_SEARCH){search=search.substring(0,Math.min(search.length(),value));while(search.length()<value)search+=" ";}
        else if(id>=4&&id<4+search.length()&&value>=32&&value<=126){StringBuilder text=new StringBuilder(search);text.setCharAt(id-4,(char)value);search=text.toString();}
    }

    @Override public void detectAndSendChanges(){if(!client&&canInteractWith(owner)){long tick=owner.world.getTotalWorldTime();if(tick!=lastRefresh&&(lastRefresh==Long.MIN_VALUE||tick-lastRefresh>=10)){lastRefresh=tick;refresh(false);}}super.detectAndSendChanges();if(!client)sendRevision();}
    @Override public ItemStack transferStackInSlot(EntityPlayer player,int slot){return ItemStack.EMPTY;}
    @Override public ItemStack slotClick(int slot,int button,net.minecraft.inventory.ClickType mode,EntityPlayer player){
        if(client||!canInteractWith(player)||!(mode==net.minecraft.inventory.ClickType.PICKUP||mode==net.minecraft.inventory.ClickType.QUICK_MOVE)||((button&255)>>>1)!=revision||slot<0||slot>GROUP_CONTROL)return ItemStack.EMPTY;
        if(slot==GROUP_CONTROL){
            if(mode!=net.minecraft.inventory.ClickType.PICKUP||(button&1)!=0)return ItemStack.EMPTY;
            grouped=!grouped;page=0;refresh(true);super.detectAndSendChanges();sendRevision();return ItemStack.EMPTY;
        }
        if(slot>=SEARCH_CLEAR){
            if(mode!=net.minecraft.inventory.ClickType.PICKUP||(button&1)!=0)return ItemStack.EMPTY;
            String next=search;
            if(slot==SEARCH_CLEAR)next="";
            else if(slot==SEARCH_BACKSPACE){if(!search.isEmpty())next=search.substring(0,search.length()-1);}
            else if(search.length()<LegacyWirelessStorage.MAX_SEARCH)next=search+(char)(32+slot-SEARCH_CHARACTER);
            if(!next.equals(search)){search=next;page=0;refresh(true);super.detectAndSendChanges();sendRevision();}
            return ItemStack.EMPTY;
        }
        if(slot==SORT_CONTROL){
            if(mode!=net.minecraft.inventory.ClickType.PICKUP)return ItemStack.EMPTY;
            sortOrder=(sortOrder+((button&1)==0?1:2))%3;page=0;refresh(true);
            super.detectAndSendChanges();sendRevision();return ItemStack.EMPTY;
        }
        long tick=owner.world.getTotalWorldTime();if(lastAction==tick)return ItemStack.EMPTY;lastAction=tick;
        try{int amount=(button&1)==1?1:64;
            if(slot<45){int at=page*45+slot;if(at<entries.size())LegacyWirelessStorage.withdrawRow(owner,held(),entries.get(at),amount);}
            else{int index=slot<72?slot-45+9:slot-72;LegacyWirelessStorage.deposit(owner,held(),index,amount);}
            refresh(false);
        }catch(RuntimeException provider){failed=true;}
        super.detectAndSendChanges();sendRevision();return ItemStack.EMPTY;
    }
    @Override public boolean enchantItem(EntityPlayer player,int button){
        if(client||!canInteractWith(player)||((button&255)>>>1)!=revision)return false;
        page=Math.max(0,page+((button&1)==0?-1:1));refresh(true);detectAndSendChanges();return true;
    }
}
