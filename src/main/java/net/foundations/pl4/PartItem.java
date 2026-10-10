package net.foundations.pl4;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;

import net.minecraft.util.EnumActionResult;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.world.World;
import net.foundations.pl4.compat.DataComponents;
import net.foundations.pl4.compat.CustomData;
import net.minecraft.nbt.NBTTagCompound;
import net.foundations.pl4.core.PartItemDataRules;

public final class PartItem extends Item {
    public final Kind kind;
    public PartItem(Kind k,Properties p) { super(p); kind=k; }
    @Override public EnumActionResult useOn(ItemUseContext c) {
        World l=c.getLevel(); var player=c.getPlayer(); if(player==null)return EnumActionResult.PASS;
        BlockPos pos=c.getClickedPos();
        HostEntity host=l.getBlockEntity(pos) instanceof HostEntity h?h:null;
        // On a cable, mount on the side the user actually clicked. On a normal block,
        // place into the adjacent cell with the attachment's back against that block.
        EnumFacing face=host==null?c.getClickedFace().getOpposite():c.getClickedFace();
        boolean outward=host!=null;
        var hit=new net.minecraft.util.math.RayTraceResult(c.getClickLocation(),c.getClickedFace(),pos,false);
        Part clicked=host==null?null:host.hit(hit);
        EnumFacing placementDirection=kind.cable()&&clicked!=null&&clicked.kind.cable()
            ?host.cableDirection(hit):c.getClickedFace();
        boolean extend=kind==Kind.LARGE_DISPLAY&&clicked!=null&&clicked.kind==Kind.LARGE_DISPLAY&&!player.isSneaking();
        Part.DisplaySettings inherited=null;long inheritedRevision=0;
        if(extend){
            if(!host.canEdit(player)||!l.mayInteract(player,pos))return EnumActionResult.FAIL;
            var point=c.getClickLocation().subtract(net.foundations.pl4.compat.PortVectors.atLowerCornerOf(pos));
            var direction=net.foundations.pl4.core.DisplayPlacement.extension(clicked.face.ordinal(),clicked.displayOutward,c.getClickedFace().ordinal(),point.x,point.y,point.z);
            if(direction.isEmpty()){
                if(!l.isClientSide)player.displayClientMessage(new net.minecraft.util.text.TextComponentString("Aim at a large display edge to extend it. Sneak-place for an independent panel."),true);
                return EnumActionResult.FAIL;
            }
            if(!l.isClientSide){
                if(l instanceof net.minecraft.world.WorldServer current)NetworkEngine.ensureCurrent(current.getServer());
                if(!(player instanceof net.minecraft.entity.player.EntityPlayerMP serverPlayer)||!DisplayNetworks.canEditCanvas(serverPlayer,host,clicked))return EnumActionResult.FAIL;
                var controller=DisplayNetworks.controller(host,clicked).part();inherited=controller.displaySettings();inheritedRevision=controller.layoutRevision;
            }
            face=clicked.face;outward=clicked.displayOutward;pos=pos.relative(EnumFacing.from3DDataValue(direction.getAsInt()));
            if(!l.hasChunkAt(pos)||l.isOutsideBuildHeight(pos)||!l.getWorldBorder().isWithinBounds(pos))return EnumActionResult.FAIL;
            if(!DisplayNetworks.canExtendAt(l,pos,clicked,player)){
                if(!l.isClientSide)player.displayClientMessage(new net.minecraft.util.text.TextComponentString("Cannot extend: the joined area is protected or exceeds 16 x 16 tiles."),true);
                return EnumActionResult.FAIL;
            }
            host=l.getBlockEntity(pos) instanceof HostEntity h?h:null;
        }
        boolean attachToReader=kind.panelDisplay()&&clicked!=null&&clicked.kind.reader()&&c.getClickedFace()==clicked.face;
        if(attachToReader){face=clicked.face;outward=true;}
        Part candidate=placementPart(c,face,outward,extend);
        if(attachToReader&&!HostBlock.canAdd(host,candidate))return EnumActionResult.FAIL;
        if(!extend&&(host==null||!HostBlock.canAdd(host,candidate))) {
            pos=pos.relative(placementDirection);face=placementDirection.getOpposite();outward=false;
            if(!l.hasChunkAt(pos))return EnumActionResult.FAIL;
            host=l.getBlockEntity(pos) instanceof HostEntity h?h:null;
            candidate=placementPart(c,face,outward,extend);
        }
        Part part=candidate;
        if(l.isOutsideBuildHeight(pos)||!l.getWorldBorder().isWithinBounds(pos)||!l.hasChunkAt(pos)||!player.mayUseItemAt(pos,c.getClickedFace(),c.getItemInHand())||!l.mayInteract(player,pos))return EnumActionResult.FAIL;
        if(host!=null&&(!host.canEdit(player)||!HostBlock.canAdd(host,part)))return EnumActionResult.FAIL;
        if(host==null&&!l.getBlockState(pos).getMaterial().isReplaceable())return EnumActionResult.FAIL;
        if(l.isClientSide)return EnumActionResult.SUCCESS;
        if(host==null) {
            if(!l.setBlock(pos,FoundationsPL4.HOST.get().defaultBlockState(),3))return EnumActionResult.FAIL;
            if(!(l.getBlockEntity(pos) instanceof HostEntity h))return EnumActionResult.FAIL;
            host=h;
        }
        if(extend){ // Edge extension explicitly adopts the existing plane/front, even for a saved item.
            part.displayOutward=outward;part.owner=clicked.owner;
            if(inherited!=null)part.applyDisplaySettings(inherited,inheritedRevision);
        }
        host.parts.put(part.slot(),part);host.changed();
        if(part.kind.cable())NetworkEngine.refreshCableGeometry(host);
        else if(l instanceof net.minecraft.world.WorldServer server)NetworkEngine.ensureCurrent(server.getServer());
        if(!player.abilities.instabuild)c.getItemInHand().shrink(1);
        return EnumActionResult.SUCCESS;
    }
    /** Resolve saved data and base orientation before BOTH overlap checks, not after slot selection. */
    private Part placementPart(ItemUseContext c,EnumFacing face,boolean outward,boolean extending){
        var player=c.getPlayer();Part part=new Part(kind,face,player.getUUID());part.displayOutward=outward;
        if(part.hologram())part.hologramView=net.foundations.pl4.core.HologramProjection.view(face.ordinal(),player.getDirection().getOpposite().ordinal());
        var saved=net.foundations.pl4.compat.PortData.get(c.getItemInHand(),DataComponents.CUSTOM_DATA);
        if(saved!=null&&saved.contains("pl_part")){
            var tag=saved.copyTag().getCompound("pl_part");tag.putString("kind",kind.id);tag.putInt("face",face.ordinal());
            Part restored=Part.load(tag,null);
            if(restored!=null){part=restored;part.owner=player.getUUID();part.identity=java.util.UUID.randomUUID();}
        }
        if(extending)part.displayOutward=outward;
        return part;
    }
    @Override public String getDescriptionId() { return "block."+FoundationsPL4.ID+"."+kind.id; }
    /** Normal block-break drop. Unconfigured parts carry no CustomData and therefore stack normally.
     * Transfer escrow is never discarded: an escrow-only payload is retained when needed. */
    public static ItemStack stack(Part part,Object registry) {
        ItemStack stack=new ItemStack(FoundationsPL4.PART_ITEMS.get(part.kind).get());
        if(PartItemDataRules.needsEscrowPayload(!part.pendingItem.isEmpty(),!part.pendingFluid.isEmpty(),part.energyCredits()>0?1:0)){
            Part escrow=new Part(part.kind,EnumFacing.DOWN,null);escrow.pendingItem=part.pendingItem.copy();escrow.pendingFluid=part.pendingFluid.copy();escrow.pendingEnergy=Math.max(0,part.pendingEnergy);
            if(part.energyCredits()>0){
                escrow.energyCredits(part.energyCredits());escrow.pendingEnergyUnit=part.pendingEnergyUnit;
                escrow.pendingEnergyJRate=part.pendingEnergyJRate;escrow.pendingEnergyEURate=part.pendingEnergyEURate;escrow.pendingEnergyEDRate=part.pendingEnergyEDRate;
                escrow.energyInput=part.energyInput;escrow.energyOutput=part.energyOutput;escrow.energyConvert=part.energyConvert;escrow.energyVoltage=part.energyVoltage;escrow.transferMode=part.transferMode;
            }
            putSaved(stack,canonical(escrow.save(registry,false)));
        }
        return stack;
    }
    /** Operator removal deliberately preserves meaningful configuration, but never runtime identity/owner/ticks/signal/revision. */
    public static ItemStack savedStack(Part part,Object registry) {
        ItemStack stack=new ItemStack(FoundationsPL4.PART_ITEMS.get(part.kind).get());putSaved(stack,canonical(part.save(registry,false)));return stack;
    }
    private static void putSaved(ItemStack stack,NBTTagCompound tag){CustomData.update(DataComponents.CUSTOM_DATA,stack,t->t.put("pl_part",tag));}
    static NBTTagCompound canonical(NBTTagCompound tag){
        NBTTagCompound out=tag.copy();for(String key:PartItemDataRules.VOLATILE_KEYS)out.remove(key);out.putInt("face",EnumFacing.DOWN.ordinal());return out;
    }
}
