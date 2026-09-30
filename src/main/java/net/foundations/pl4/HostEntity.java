package net.foundations.pl4;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

public final class HostEntity extends BlockEntity {
    public final Map<Integer,Part> parts=new TreeMap<>();
    private CompoundTag lastSync;
    private final int[] cableConnections = new int[6];
    private net.minecraft.world.phys.shapes.VoxelShape cachedOutline;
    private int externalLeads;
    public boolean externalLead(Part p){return (externalLeads&(1<<p.slot()))!=0;}
    public void setExternalLeads(int mask){mask&=8191;if(mask!=externalLeads){externalLeads=mask;cachedOutline=null;}}
    public boolean readerHasDisplay(Part p){return MultipartShapes.paired(parts.values(),p);}
    public Part coveredReader(Part display){
        if(!display.kind.panelDisplay())return null;
        Part reader=parts.get(display.face.ordinal());return reader!=null&&reader.kind.reader()?reader:null;
    }
    public Part interactionTarget(BlockHitResult hit,boolean behindDisplay){
        Part p=hit(hit);if(p!=null&&behindDisplay){Part reader=coveredReader(p);if(reader!=null)return reader;}return p;
    }
    public int connection(Direction face) { return cableConnections[face.ordinal()]; }
    public void setConnections(int[] values) {
        if(!java.util.Arrays.equals(cableConnections,values)) { System.arraycopy(values,0,cableConnections,0,6); cachedOutline=null; }
    }
    public net.minecraft.world.phys.shapes.VoxelShape outline() {
        if(cachedOutline==null) {
            var shape=net.minecraft.world.phys.shapes.Shapes.empty();
            for(Part p:parts.values()){
                shape=net.minecraft.world.phys.shapes.Shapes.or(shape,MultipartShapes.part(parts.values(),p));
                if(externalLead(p))shape=net.minecraft.world.phys.shapes.Shapes.or(shape,MultipartShapes.lead(p));
            }
            if(parts.containsKey(6))for(Direction f:Direction.values())shape=net.minecraft.world.phys.shapes.Shapes.or(shape,PartShapes.arm(connection(f),f));
            cachedOutline=shape.isEmpty()?PartShapes.CENTRE:shape.optimize();
        }
        return cachedOutline;
    }
    public HostEntity(BlockPos p,BlockState s){super(FoundationsPL4.HOST_ENTITY.get(),p,s);}
    @Override public void onLoad(){super.onLoad();if(level!=null&&!level.isClientSide)NetworkEngine.add(this);}
    @Override public void setRemoved(){if(level!=null&&!level.isClientSide)NetworkEngine.remove(this);super.setRemoved();}
    public boolean canEdit(Player p){return p.hasPermissions(2)||parts.values().stream().allMatch(a->a.owner==null||a.owner.equals(p.getUUID()));}
    public Part hit(BlockHitResult h){
        Vec3 local=h.getLocation().subtract(Vec3.atLowerCornerOf(worldPosition));
        // A front hit belongs to the thin display, never its covered reader; use real paired geometry.
        for(boolean display:new boolean[]{true,false})for(Part p:parts.values())if(!p.kind.cable()&&p.kind.display()==display)
            for(AABB b:MultipartShapes.part(parts.values(),p).toAabbs())if(b.inflate(.0001).contains(local))return p;
        for(Part p:parts.values())if(externalLead(p))for(AABB b:MultipartShapes.lead(p).toAabbs())if(b.inflate(.0001).contains(local))return p;
        return parts.getOrDefault(6,parts.values().stream().findFirst().orElse(null));
    }
    public Direction cableDirection(BlockHitResult hit){
        Vec3 point=hit.getLocation().subtract(Vec3.atCenterOf(worldPosition));
        double x=Math.abs(point.x),y=Math.abs(point.y),z=Math.abs(point.z);
        if(Math.max(x,Math.max(y,z))<=.126)return hit.getDirection();
        if(x>=y&&x>=z)return point.x<0?Direction.WEST:Direction.EAST;
        if(y>=z)return point.y<0?Direction.DOWN:Direction.UP;
        return point.z<0?Direction.NORTH:Direction.SOUTH;
    }
    public void changed(){cachedOutline=null;setChanged();if(level!=null){NetworkEngine.invalidate(level);level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);level.updateNeighborsAt(worldPosition,getBlockState().getBlock());}}
    public void syncIfChanged(){
        if(level==null||level.isClientSide)return;
        CompoundTag tag=getUpdateTag(level.registryAccess());
        if(!tag.equals(lastSync)){lastSync=tag;level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
    }
    public int output(Direction side){
        return parts.values().stream().filter(p->p.kind==Kind.SIGNALLER||p.kind==Kind.REDSTONE_RECEIVER||p.kind==Kind.CLOCK).mapToInt(p->p.signal).max().orElse(0);
    }
    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);write(t,r,false);}
    private void write(CompoundTag t,HolderLookup.Provider r,boolean sync){
        ListTag list=new ListTag();parts.values().forEach(p->list.add(p.save(r,sync)));t.put("parts",list);t.putInt("schema",2);
        if(sync){t.putIntArray("cableConnections",cableConnections);t.putInt("externalLeads",externalLeads);}
    }
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){
        super.loadAdditional(t,r);cachedOutline=null;java.util.Arrays.fill(cableConnections,0);
        int[] arms=t.getIntArray("cableConnections");if(arms.length==6)for(int a=0;a<6;a++)cableConnections[a]=Math.clamp(arms[a],0,3);
        externalLeads=t.getInt("externalLeads")&8191;parts.clear();ListTag list=t.getList("parts",Tag.TAG_COMPOUND);
        // R6 stored kind+face, not a persisted slot key. Reindex displays without changing identity/layout.
        for(int i=0;i<Math.min(list.size(),net.foundations.pl4.core.MultipartTopology.SLOT_COUNT);i++){
            Part p=Part.load(list.getCompound(i),r);if(p!=null)parts.put(p.slot(),p);
        }
        if(level!=null&&!level.isClientSide)NetworkEngine.invalidate(level);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){CompoundTag t=new CompoundTag();write(t,r,true);return t;}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
