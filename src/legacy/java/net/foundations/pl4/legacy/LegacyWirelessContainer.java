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
    private List<LegacyWirelessStorage.Entry> entries=new ArrayList<LegacyWirelessStorage.Entry>();
    public int page=0,revision=-1;private long lastAction=Long.MIN_VALUE,lastRefresh=Long.MIN_VALUE;private boolean failed;
    public LegacyWirelessContainer(EntityPlayer player,boolean client){
        this.owner=player;this.client=client;this.toolSlot=player.inventory.currentItem;ItemStack held=player.inventory.getStackInSlot(toolSlot);this.binding=LegacyTile.STACKS.empty(held)?null:held.copy();
        for(int i=0;i<45;i++)addSlotToContainer(locked(view,i,8+(i%9)*18,18+(i/9)*18));
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlotToContainer(locked(player.inventory,col+row*9+9,8+col*18,122+row*18));
        for(int i=0;i<9;i++)addSlotToContainer(locked(player.inventory,i,8+i*18,180));
        if(!client)refresh(false);
    }
    private static Slot locked(net.minecraft.inventory.IInventory inventory,int i,int x,int y){return new Slot(inventory,i,x,y){public boolean isItemValid(ItemStack stack){return false;}public boolean canTakeStack(EntityPlayer player){return false;}};}
    private ItemStack held(){return owner.inventory.getStackInSlot(toolSlot);}
    @Override public boolean canInteractWith(EntityPlayer player){return player==owner&&!failed&&(client||owner.inventory.currentItem==toolSlot&&LegacyWirelessStorage.tool(held())&&ItemStack.areItemStackTagsEqual(binding,held())&&LegacyWirelessStorage.resolve(owner,held())!=null);}
    private void refresh(boolean force){
        List<LegacyWirelessStorage.Entry> next=LegacyWirelessStorage.entries(owner,held());boolean changed=force||next.size()!=entries.size();
        if(!changed)for(int i=0;i<next.size();i++){LegacyWirelessStorage.Entry a=entries.get(i),b=next.get(i);if(a.node!=b.node||a.slot!=b.slot||!ItemStack.areItemStacksEqual(a.snapshot,b.snapshot)){changed=true;break;}}
        if(revision<0||changed){if(revision>=127){failed=true;return;}revision++;}
        entries=next;page=Math.max(0,Math.min(page,Math.max(0,(entries.size()-1)/45)));
        for(int i=0;i<45;i++){int at=page*45+i;view.setInventorySlotContents(i,at<entries.size()?entries.get(at).snapshot.copy():null);}
    }
    private void sendRevision(){for(Object listener:crafters)((net.minecraft.inventory.ICrafting)listener).sendProgressBarUpdate(this,0,revision);}
    @Override public void updateProgressBar(int id,int value){if(id==0)revision=value;}

    @Override public void detectAndSendChanges(){if(!client&&canInteractWith(owner)){long tick=owner.worldObj.getTotalWorldTime();if(tick!=lastRefresh&&(lastRefresh==Long.MIN_VALUE||tick-lastRefresh>=10)){lastRefresh=tick;refresh(false);}}super.detectAndSendChanges();if(!client)sendRevision();}
    @Override public ItemStack transferStackInSlot(EntityPlayer player,int slot){return null;}
    @Override public ItemStack slotClick(int slot,int button,int mode,EntityPlayer player){
        if(client||!canInteractWith(player)||!(mode==0||mode==1)||((button&255)>>>1)!=revision||slot<0||slot>=81)return null;
        long tick=owner.worldObj.getTotalWorldTime();if(lastAction==tick)return null;lastAction=tick;
        try{int amount=(button&1)==1?1:64;
            if(slot<45){int at=page*45+slot;if(at<entries.size())LegacyWirelessStorage.withdraw(owner,held(),entries.get(at),amount);}
            else{int index=slot<72?slot-45+9:slot-72;LegacyWirelessStorage.deposit(owner,held(),index,amount);}
            refresh(false);
        }catch(RuntimeException provider){failed=true;}
        super.detectAndSendChanges();sendRevision();return null;
    }
    @Override public boolean enchantItem(EntityPlayer player,int button){
        if(client||!canInteractWith(player)||((button&255)>>>1)!=revision)return false;
        page=Math.max(0,page+((button&1)==0?-1:1));refresh(true);detectAndSendChanges();return true;
    }
}
