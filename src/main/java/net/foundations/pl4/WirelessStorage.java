package net.foundations.pl4;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.foundations.pl4.compat.TransferAdapters.IItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;

/** Held-tool sessions. Every action resolves the current sided capability without loading chunks. */
public final class WirelessStorage {
    public static final int PAGE_SIZE=6,MAX_SLOTS=65536;
    private record Target(Part node,IItemHandler inventory){}
    private record Session(UUID token,InteractionHand hand,ItemStack tool,Part.Link binding,int page,List<ItemStack> shown,int opened){}
    private record Rate(int tick,int count){}
    private static final Map<ServerPlayer,Session> SESSIONS=new WeakHashMap<>();
    private static final Map<ServerPlayer,Rate> RATES=new WeakHashMap<>();
    private static Target resolve(ServerPlayer player,Part.Link binding){
        if(player.isSpectator()||binding==null||binding.part()==null||binding.entity()!=null||!PLConfig.WIRELESS.get()||!PLConfig.TRANSFERS.get())return null;
        if(!PLConfig.CROSS_DIMENSION.get()&&!player.level().dimension().identifier().toString().equals(binding.dimension()))return null;
        var server=player.level().getServer();if(!NetworkEngine.loaded(server,binding))return null;
        var world=NetworkEngine.level(server,binding);
        if(!(world.getBlockEntity(binding.pos()) instanceof HostEntity host)||!host.canEdit(player)||!world.mayInteract(player,binding.pos()))return null;
        Part node=host.parts.values().stream().filter(p->p.identity.equals(binding.part())).findFirst().orElse(null);
        if(node==null||(node.kind!=Kind.NODE&&node.kind!=Kind.TRANSFER_NODE))return null;
        BlockPos target=host.getBlockPos().relative(node.face);
        if(!world.hasChunkAt(target)||!world.mayInteract(player,target))return null;
        IItemHandler inventory=net.foundations.pl4.compat.TransferAdapters.items(world,target,node.face.getOpposite());
        return inventory==null?null:new Target(node,inventory);
    }
    public static UUID open(ServerPlayer player,InteractionHand hand){
        ItemStack tool=player.getItemInHand(hand);Part.Link binding=ToolItem.link(tool);
        if(!tool.is(FoundationsPL4.item("wirelessstorage"))){SESSIONS.remove(player);return null;}
        Target target=resolve(player,binding);
        if(target==null){SESSIONS.remove(player);player.sendOverlayMessage(Component.literal("Bind Wireless Storage to your loaded inventory Node. Wireless and transfers must be enabled."));return null;}
        return send(player,hand,tool,binding,target,0,false,"");
    }
    static void request(ServerPlayer player,PLPackets.StorageRequest packet){
        Session session=SESSIONS.get(player);
        if(session==null||!session.token().equals(packet.token())||player.tickCount-session.opened()>1200)return;
        Rate rate=RATES.get(player);if(rate!=null&&rate.tick()==player.tickCount&&rate.count()>=4)return;
        RATES.put(player,new Rate(player.tickCount,rate!=null&&rate.tick()==player.tickCount?rate.count()+1:1));
        SESSIONS.remove(player); // A token authorizes one request; duplicate packets cannot move twice.
        ItemStack held=player.getItemInHand(session.hand());
        if(held!=session.tool()||!held.is(FoundationsPL4.item("wirelessstorage"))||!Objects.equals(ToolItem.link(held),session.binding()))return;
        Target target=resolve(player,session.binding());if(target==null){sendClosed(player,"Target unavailable or permission changed.");return;}
        int page=session.page();String status="";
        if(packet.action().equals("page"))page=Math.clamp(packet.index(),0,(Math.max(1,Math.min(MAX_SLOTS,target.inventory().getSlots()))-1)/PAGE_SIZE);
        else if(packet.action().equals("withdraw")){
            int slot=packet.index(),local=slot-page*PAGE_SIZE;
            if(local<0||local>=session.shown().size()||slot>=target.inventory().getSlots()||packet.amount()<1||packet.amount()>64){sendClosed(player,"Invalid storage request.");return;}
            ItemStack expected=session.shown().get(local),current=target.inventory().getStackInSlot(slot);
            if(expected.isEmpty()||!ItemStack.isSameItemSameComponents(expected,current))status="Slot changed; refreshed without moving items.";
            else if(!TransferFilters.items(current,target.node(),false))status="Node send filter blocks this item.";
            else{
                int amount=Math.min(packet.amount(),room(player,current));
                ItemStack trial=target.inventory().extractItem(slot,amount,true);
                if(amount>0&&!trial.isEmpty()&&ItemStack.isSameItemSameComponents(current,trial)){
                    ItemStack extracted=target.inventory().extractItem(slot,Math.min(amount,trial.getCount()),false);
                    if(!extracted.isEmpty()){player.getInventory().add(extracted);if(!extracted.isEmpty())player.drop(extracted,false,net.minecraft.util.Prediction.SERVER_ONLY);}
                    player.getInventory().setChanged();player.inventoryMenu.broadcastChanges();
                }else status="No inventory space or extraction is blocked.";
            }
        }else if(packet.action().equals("deposit")){
            if(session.hand()!=InteractionHand.MAIN_HAND){status="Hold Wireless Storage in your main hand; put items in your offhand.";}
            else{
                ItemStack offhand=player.getOffhandItem();
                if(offhand.isEmpty())status="Put the stack to deposit in your offhand.";
                else if(!TransferFilters.items(offhand,target.node(),true))status="Node receive filter blocks this item.";
                else{
                    int moved=0;for(int slot=0;slot<Math.min(MAX_SLOTS,target.inventory().getSlots())&&!offhand.isEmpty();slot++){
                        ItemStack offered=offhand.copy();ItemStack simulated=target.inventory().insertItem(slot,offered,true);int capacity=offered.getCount()-simulated.getCount();if(capacity<=0)continue;
                        ItemStack send=offhand.copyWithCount(Math.min(capacity,offhand.getCount()));ItemStack remainder=target.inventory().insertItem(slot,send,false);
                        int accepted=Math.clamp(send.getCount()-remainder.getCount(),0,send.getCount());offhand.shrink(accepted);moved+=accepted;
                    }
                    player.getInventory().setChanged();player.inventoryMenu.broadcastChanges();status="Deposited "+moved+" item(s).";
                }
            }
        }else if(!packet.action().equals("refresh")){sendClosed(player,"Unknown storage action.");return;}
        send(player,session.hand(),held,session.binding(),target,page,true,status);
    }
    private static int room(ServerPlayer player,ItemStack stack){
        int room=0;for(int i=0;i<36;i++){ItemStack existing=player.getInventory().getItem(i);if(existing.isEmpty())room+=stack.getMaxStackSize();else if(ItemStack.isSameItemSameComponents(existing,stack))room+=Math.max(0,Math.min(existing.getMaxStackSize(),stack.getMaxStackSize())-existing.getCount());if(room>=64)return 64;}return room;
    }
    private static UUID send(ServerPlayer player,InteractionHand hand,ItemStack tool,Part.Link binding,Target target,int page,boolean reply,String status){
        int slots=Math.clamp(target.inventory().getSlots(),0,MAX_SLOTS);page=Math.clamp(page,0,Math.max(0,(slots-1)/PAGE_SIZE));
        UUID token=UUID.randomUUID();List<ItemStack> shown=new ArrayList<>();ListTag rows=new ListTag();
        for(int slot=page*PAGE_SIZE;slot<Math.min(slots,(page+1)*PAGE_SIZE);slot++){
            ItemStack stack=target.inventory().getStackInSlot(slot).copy();shown.add(stack);CompoundTag row=new CompoundTag();row.putInt("slot",slot);row.putString("name",stack.isEmpty()?"Empty":stack.getHoverName().getString());row.putInt("count",stack.getCount());rows.add(row);
        }
        SESSIONS.put(player,new Session(token,hand,tool,binding,page,List.copyOf(shown),player.tickCount));
        CompoundTag tag=new CompoundTag();tag.putBoolean("wirelessStorage",true);tag.putBoolean("reply",reply);tag.putString("token",token.toString());tag.putInt("page",page);tag.putInt("slots",slots);tag.put("storageRows",rows);tag.putString("status",status);
        PacketDistributor.sendToPlayer(player,new PLPackets.Open(BlockPos.ZERO,-1,tag));return token;
    }
    private static void sendClosed(ServerPlayer player,String status){CompoundTag tag=new CompoundTag();tag.putBoolean("wirelessStorage",true);tag.putBoolean("reply",true);tag.putBoolean("closed",true);tag.putString("status",status);PacketDistributor.sendToPlayer(player,new PLPackets.Open(BlockPos.ZERO,-1,tag));}
    private WirelessStorage(){}
}
