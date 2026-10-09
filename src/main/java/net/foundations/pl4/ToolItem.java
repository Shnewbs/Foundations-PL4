package net.foundations.pl4;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Direction;
import net.foundations.pl4.compat.DataComponents;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.foundations.pl4.compat.CustomData;
import net.minecraft.item.ItemUseContext;
import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockRayTraceResult;
import net.foundations.pl4.compat.PacketDistributor;

public final class ToolItem extends Item {
    public enum Mode{OPERATOR,BLOCK_LINK,ENTITY_LINK,MONITOR,STORAGE,GUIDE}
    private final Mode mode;
    public ToolItem(Mode m,Properties p){super(p);mode=m;}
    static Part.Link link(ItemStack stack){var data=net.foundations.pl4.compat.PortData.get(stack,DataComponents.CUSTOM_DATA);return data==null||!data.contains("pl_link")?null:Part.Link.load(data.copyTag().getCompound("pl_link"));}
    private static void save(ItemStack stack,Part.Link link){CustomData.update(DataComponents.CUSTOM_DATA,stack,t->t.put("pl_link",link.save()));}
    @Override public ActionResultType useOn(ItemUseContext c){
        if(!(c.getPlayer() instanceof ServerPlayerEntity player))return ActionResultType.SUCCESS;
        World level=c.getLevel();BlockPos pos=c.getClickedPos();
        HostEntity host=level.getBlockEntity(pos) instanceof HostEntity h?h:null;
        Part part=host==null?null:host.hit(new BlockRayTraceResult(c.getClickLocation(),c.getClickedFace(),pos,false));
        if(mode==Mode.OPERATOR&&host!=null&&part!=null){
            if(!host.canEdit(player)||!level.mayInteract(player,pos))return ActionResultType.FAIL;
            if(player.isShiftKeyDown()){
                host.parts.remove(part.slot());Block.popResource(level,pos,PartItem.savedStack(part,level.registryAccess()));
                if(host.parts.isEmpty())level.removeBlock(pos,false);else host.changed();
            }else if(part.kind.cable()) {
                Direction side=host.cableDirection(new BlockRayTraceResult(c.getClickLocation(),c.getClickedFace(),pos,false));
                part.blockedFaces=net.foundations.pl4.core.ConnectionRules.toggle(part.blockedFaces,side.ordinal());
                boolean blocked=net.foundations.pl4.core.ConnectionRules.blocked(part.blockedFaces,side.ordinal());
                BlockPos adjacent=pos.relative(side);
                if(level.hasChunkAt(adjacent)&&level.getBlockEntity(adjacent) instanceof HostEntity neighbor&&neighbor.canEdit(player)&&level.mayInteract(player,adjacent)) {
                    Part cable=neighbor.parts.get(6);
                    if(cable!=null&&cable.kind==part.kind) {
                        int bit=1<<side.getOpposite().ordinal();
                        cable.blockedFaces=blocked?cable.blockedFaces|bit:cable.blockedFaces&~bit;
                        neighbor.changed();
                    }
                }
                host.changed();
                player.displayClientMessage(new net.minecraft.util.text.StringTextComponent(side.getName()+" cable port "+(blocked?"disconnected":"enabled")),true);
            }else PLPackets.open(player,host,part);
            return ActionResultType.CONSUME;
        }
        if(mode==Mode.STORAGE){
            if(player.isShiftKeyDown()){
                if(host==null||part==null||(part.kind!=Kind.NODE&&part.kind!=Kind.TRANSFER_NODE)||!host.canEdit(player)||!level.mayInteract(player,pos))return ActionResultType.FAIL;
                save(c.getItemInHand(),new Part.Link(level.dimension().location().toString(),pos,part.face,null,part.identity));
                player.displayClientMessage(new net.minecraft.util.text.StringTextComponent("Wireless Storage bound to inventory Node"),true);
            }else WirelessStorage.open(player,c.getHand());
            return ActionResultType.CONSUME;
        }
        if(mode==Mode.BLOCK_LINK||mode==Mode.MONITOR||mode==Mode.ENTITY_LINK){
            if(player.isShiftKeyDown()){
                save(c.getItemInHand(),new Part.Link(level.dimension().location().toString(),pos,c.getClickedFace(),null,part==null?null:part.identity));
                player.displayClientMessage(new net.minecraft.util.text.StringTextComponent("Linked "+pos.toShortString()+" in "+level.dimension().location()),true);return ActionResultType.CONSUME;
            }
            Part.Link link=link(c.getItemInHand());
            if(host!=null&&part!=null&&link!=null&&(part.kind==Kind.ARRAY||part.kind==Kind.ENTITY_NODE||part.kind.receiver())){
                if(!ComponentLinks.add(player,host,part,link)){player.displayClientMessage(new net.minecraft.util.text.StringTextComponent("Link rejected: check type, owner, loaded target, dimension policy and free link slots."),true);return ActionResultType.FAIL;}
                host.changed();player.displayClientMessage(new net.minecraft.util.text.StringTextComponent("Added link ("+part.links.size()+")"),true);return ActionResultType.CONSUME;
            }
        }
        return use(level,player,c.getHand()).getResult();
    }
    @Override public ActionResultType interactLivingEntity(ItemStack stack,PlayerEntity player,LivingEntity entity,Hand hand){
        if(mode!=Mode.ENTITY_LINK)return ActionResultType.PASS;
        if(!player.level.isClientSide){save(stack,new Part.Link(entity.level.dimension().location().toString(),entity.blockPosition(),Direction.UP,entity.getUUID(),null));player.displayClientMessage(new net.minecraft.util.text.StringTextComponent("Linked "+entity.getName().getString()),true);}return ActionResultType.sidedSuccess(player.level.isClientSide);
    }
    @Override public ActionResult<ItemStack> use(World l,PlayerEntity player,Hand hand){
        ItemStack stack=player.getItemInHand(hand);
        if(player instanceof ServerPlayerEntity sp){
            if(mode==Mode.GUIDE){CompoundNBT tag=new CompoundNBT();tag.putBoolean("guide",true);PacketDistributor.sendToPlayer(sp,new PLPackets.Open(BlockPos.ZERO,-1,tag));}
            else if(mode==Mode.STORAGE)WirelessStorage.open(sp,hand);
            else if(mode==Mode.MONITOR){
                Part.Link link=link(stack);
                if(link!=null&&NetworkEngine.loaded(sp.getServer(),link)){
                    var world=NetworkEngine.level(sp.getServer(),link);
                    if(world.getBlockEntity(link.pos()) instanceof HostEntity host&&host.canEdit(player)){
                        Part target=host.parts.values().stream().filter(p->p.identity.equals(link.part())).findFirst().orElse(null);if(target!=null)PLPackets.open(sp,host,target);
                    }
                }else sp.displayClientMessage(new net.minecraft.util.text.StringTextComponent("Linked target is not loaded"),true);
            }
        }
        return ActionResult.sidedSuccess(stack,l.isClientSide);
    }
}
