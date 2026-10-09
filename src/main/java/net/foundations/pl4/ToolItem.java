package net.foundations.pl4;

import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ToolItem extends Item {
    public enum Mode{OPERATOR,BLOCK_LINK,ENTITY_LINK,MONITOR,STORAGE,GUIDE}
    private final Mode mode;
    public ToolItem(Mode m,Properties p){super(p);mode=m;}
    static Part.Link link(ItemStack stack){var data=stack.get(DataComponents.CUSTOM_DATA);return data==null||!data.contains("pl_link")?null:Part.Link.load(data.copyTag().getCompound("pl_link").orElseGet(CompoundTag::new));}
    private static void save(ItemStack stack,Part.Link link){CustomData.update(DataComponents.CUSTOM_DATA,stack,t->t.put("pl_link",link.save()));}
    @Override public InteractionResult useOn(UseOnContext c){
        if(!(c.getPlayer() instanceof ServerPlayer player))return InteractionResult.SUCCESS;
        Level level=c.getLevel();BlockPos pos=c.getClickedPos();
        HostEntity host=level.getBlockEntity(pos) instanceof HostEntity h?h:null;
        Part part=host==null?null:host.hit(new BlockHitResult(c.getClickLocation(),c.getClickedFace(),pos,false));
        if(mode==Mode.OPERATOR&&host!=null&&part!=null){
            if(!host.canEdit(player)||!level.mayInteract(player,pos))return InteractionResult.FAIL;
            if(player.isShiftKeyDown()){
                host.parts.remove(part.slot());Block.popResource(level,pos,PartItem.savedStack(part,level.registryAccess()));
                if(host.parts.isEmpty())level.removeBlock(pos,false);else host.changed();
            }else if(part.kind.cable()) {
                Direction side=host.cableDirection(new BlockHitResult(c.getClickLocation(),c.getClickedFace(),pos,false));
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
                player.sendOverlayMessage(Component.literal(side.getName()+" cable port "+(blocked?"disconnected":"enabled")));
            }else PLPackets.open(player,host,part);
            return InteractionResult.CONSUME;
        }
        if(mode==Mode.STORAGE){
            if(player.isShiftKeyDown()){
                if(host==null||part==null||(part.kind!=Kind.NODE&&part.kind!=Kind.TRANSFER_NODE)||!host.canEdit(player)||!level.mayInteract(player,pos))return InteractionResult.FAIL;
                save(c.getItemInHand(),new Part.Link(level.dimension().identifier().toString(),pos,part.face,null,part.identity));
                player.sendOverlayMessage(Component.literal("Wireless Storage bound to inventory Node"));
            }else WirelessStorage.open(player,c.getHand());
            return InteractionResult.SUCCESS;
        }
        if(mode==Mode.BLOCK_LINK||mode==Mode.MONITOR||mode==Mode.ENTITY_LINK){
            if(player.isShiftKeyDown()){
                save(c.getItemInHand(),new Part.Link(level.dimension().identifier().toString(),pos,c.getClickedFace(),null,part==null?null:part.identity));
                player.sendOverlayMessage(Component.literal("Linked "+pos.toShortString()+" in "+level.dimension().identifier()));return InteractionResult.CONSUME;
            }
            Part.Link link=link(c.getItemInHand());
            if(host!=null&&part!=null&&link!=null&&(part.kind==Kind.ARRAY||part.kind==Kind.ENTITY_NODE||part.kind.receiver())){
                if(!ComponentLinks.add(player,host,part,link)){player.sendOverlayMessage(Component.literal("Link rejected: check type, owner, loaded target, dimension policy and free link slots."));return InteractionResult.FAIL;}
                host.changed();player.sendOverlayMessage(Component.literal("Added link ("+part.links.size()+")"));return InteractionResult.CONSUME;
            }
        }
        return use(level,player,c.getHand());
    }
    @Override public InteractionResult interactLivingEntity(ItemStack stack,Player player,LivingEntity entity,InteractionHand hand){
        if(mode!=Mode.ENTITY_LINK)return InteractionResult.PASS;
        if(!player.level().isClientSide()){save(stack,new Part.Link(entity.level().dimension().identifier().toString(),entity.blockPosition(),Direction.UP,entity.getUUID(),null));player.sendOverlayMessage(Component.literal("Linked "+entity.getName().getString()));}return InteractionResult.SUCCESS;
    }
    @Override public InteractionResult use(Level l,Player player,InteractionHand hand){
        ItemStack stack=player.getItemInHand(hand);
        if(player instanceof ServerPlayer sp){
            if(mode==Mode.GUIDE){CompoundTag tag=new CompoundTag();tag.putBoolean("guide",true);PacketDistributor.sendToPlayer(sp,new PLPackets.Open(BlockPos.ZERO,-1,tag));}
            else if(mode==Mode.STORAGE)WirelessStorage.open(sp,hand);
            else if(mode==Mode.MONITOR){
                Part.Link link=link(stack);
                if(link!=null&&NetworkEngine.loaded(sp.level().getServer(),link)){
                    var world=NetworkEngine.level(sp.level().getServer(),link);
                    if(world.getBlockEntity(link.pos()) instanceof HostEntity host&&host.canEdit(player)){
                        Part target=host.parts.values().stream().filter(p->p.identity.equals(link.part())).findFirst().orElse(null);if(target!=null)PLPackets.open(sp,host,target);
                    }
                }else sp.sendOverlayMessage(Component.literal("Linked target is not loaded"));
            }
        }
        return InteractionResult.SUCCESS;
    }
}
