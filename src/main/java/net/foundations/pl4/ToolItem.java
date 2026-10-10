package net.foundations.pl4;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;
import net.foundations.pl4.compat.DataComponents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.foundations.pl4.compat.CustomData;
import net.minecraft.item.ItemUseContext;
import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.foundations.pl4.compat.PacketDistributor;

public final class ToolItem extends Item {
    public enum Mode{OPERATOR,BLOCK_LINK,ENTITY_LINK,MONITOR,STORAGE,GUIDE}
    private final Mode mode;
    public ToolItem(Mode m,Properties p){super(p);mode=m;}
    static Part.Link link(ItemStack stack){var data=net.foundations.pl4.compat.PortData.get(stack,DataComponents.CUSTOM_DATA);return data==null||!data.contains("pl_link")?null:Part.Link.load(data.copyTag().getCompound("pl_link"));}
    private static void save(ItemStack stack,Part.Link link){CustomData.update(DataComponents.CUSTOM_DATA,stack,t->t.put("pl_link",link.save()));}
    @Override public EnumActionResult useOn(ItemUseContext c){
        if(!(c.getPlayer() instanceof EntityPlayerMP player))return EnumActionResult.SUCCESS;
        World level=c.getLevel();BlockPos pos=c.getClickedPos();
        HostEntity host=level.getBlockEntity(pos) instanceof HostEntity h?h:null;
        Part part=host==null?null:host.hit(new RayTraceResult(c.getClickLocation(),c.getClickedFace(),pos,false));
        if(mode==Mode.OPERATOR&&host!=null&&part!=null){
            if(!host.canEdit(player)||!level.mayInteract(player,pos))return EnumActionResult.FAIL;
            if(player.isSneaking()){
                host.parts.remove(part.slot());Block.popResource(level,pos,PartItem.savedStack(part,null));
                if(host.parts.isEmpty())level.removeBlock(pos,false);else host.changed();
            }else if(part.kind.cable()) {
                EnumFacing side=host.cableDirection(new RayTraceResult(c.getClickLocation(),c.getClickedFace(),pos,false));
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
                player.displayClientMessage(new net.minecraft.util.text.TextComponentString(side.getName()+" cable port "+(blocked?"disconnected":"enabled")),true);
            }else PLPackets.open(player,host,part);
            return EnumActionResult.SUCCESS;
        }
        if(mode==Mode.STORAGE){
            if(player.isSneaking()){
                if(host==null||part==null||(part.kind!=Kind.NODE&&part.kind!=Kind.TRANSFER_NODE)||!host.canEdit(player)||!level.mayInteract(player,pos))return EnumActionResult.FAIL;
                save(c.getItemInHand(),new Part.Link(level.dimension.getType().getRegistryName().toString(),pos,part.face,null,part.identity));
                player.displayClientMessage(new net.minecraft.util.text.TextComponentString("Wireless Storage bound to inventory Node"),true);
            }else WirelessStorage.open(player,c.getHand());
            return EnumActionResult.SUCCESS;
        }
        if(mode==Mode.BLOCK_LINK||mode==Mode.MONITOR||mode==Mode.ENTITY_LINK){
            if(player.isSneaking()){
                save(c.getItemInHand(),new Part.Link(level.dimension.getType().getRegistryName().toString(),pos,c.getClickedFace(),null,part==null?null:part.identity));
                player.displayClientMessage(new net.minecraft.util.text.TextComponentString("Linked "+pos.toString()+" in "+level.dimension.getType().getRegistryName()),true);return EnumActionResult.SUCCESS;
            }
            Part.Link link=link(c.getItemInHand());
            if(host!=null&&part!=null&&link!=null&&(part.kind==Kind.ARRAY||part.kind==Kind.ENTITY_NODE||part.kind.receiver())){
                if(!ComponentLinks.add(player,host,part,link)){player.displayClientMessage(new net.minecraft.util.text.TextComponentString("Link rejected: check type, owner, loaded target, dimension policy and free link slots."),true);return EnumActionResult.FAIL;}
                host.changed();player.displayClientMessage(new net.minecraft.util.text.TextComponentString("Added link ("+part.links.size()+")"),true);return EnumActionResult.SUCCESS;
            }
        }
        return use(level,player,c.getHand()).getResult();
    }
    /** Native 1.15 Item/ItemStack entity-use hook returns whether the action was handled. */
    @Override public boolean interactEnemy(ItemStack stack,EntityPlayer player,LivingEntity entity,EnumHand hand){
        if(mode!=Mode.ENTITY_LINK)return false;
        if(!player.level.isClientSide){
            save(stack,new Part.Link(entity.level.dimension.getType().getRegistryName().toString(),entity.getCommandSenderBlockPosition(),EnumFacing.UP,entity.getUUID(),null));
            player.displayClientMessage(new net.minecraft.util.text.TextComponentString("Linked "+entity.getName().getString()),true);
        }
        return true;
    }
    @Override public ActionResult<ItemStack> use(World l,EntityPlayer player,EnumHand hand){
        ItemStack stack=player.getItemInHand(hand);
        if(player instanceof EntityPlayerMP sp){
            if(mode==Mode.GUIDE){NBTTagCompound tag=new NBTTagCompound();tag.putBoolean("guide",true);PacketDistributor.sendToPlayer(sp,new PLPackets.Open(BlockPos.ZERO,-1,tag));}
            else if(mode==Mode.STORAGE)WirelessStorage.open(sp,hand);
            else if(mode==Mode.MONITOR){
                Part.Link link=link(stack);
                if(link!=null&&NetworkEngine.loaded(sp.getServer(),link)){
                    var world=NetworkEngine.level(sp.getServer(),link);
                    if(world.getBlockEntity(link.pos()) instanceof HostEntity host&&host.canEdit(player)){
                        Part target=host.parts.values().stream().filter(p->p.identity.equals(link.part())).findFirst().orElse(null);if(target!=null)PLPackets.open(sp,host,target);
                    }
                }else sp.displayClientMessage(new net.minecraft.util.text.TextComponentString("Linked target is not loaded"),true);
            }
        }
        return net.foundations.pl4.compat.PortInteractions.sidedSuccess(stack,l.isClientSide);
    }
}
