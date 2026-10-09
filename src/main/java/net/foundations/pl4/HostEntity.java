package net.foundations.pl4;

import java.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;

public final class HostEntity extends TileEntity {
    public static java.util.function.Function<HostEntity,net.minecraft.util.math.AxisAlignedBB> clientRenderBounds=h->new net.minecraft.util.math.AxisAlignedBB(h.getBlockPos());
    @Override public net.minecraft.util.math.AxisAlignedBB getRenderBoundingBox(){return clientRenderBounds.apply(this);}

    public final Map<Integer,Part> parts=new TreeMap<>();
    private CompoundNBT lastSync;
    private record CableSync(Part part,String status,int signal,List<Part.Row> rows) {}
    private CableSync lastCableSync;
    private long syncTagBuilds;
    // Package-private diagnostic for executable allocation-regression fixtures.
    long syncTagBuildCount(){return syncTagBuilds;}

    private final int[] cableConnections = new int[6];
    private net.minecraft.util.math.shapes.VoxelShape cachedOutline;
    private int externalLeads;
    public boolean externalLead(Part p){return (externalLeads&(1<<p.slot()))!=0;}
    public void setExternalLeads(int mask){mask&=8191;if(mask!=externalLeads){externalLeads=mask;cachedOutline=null;lastCableSync=null;}}
    public boolean readerHasDisplay(Part p){
        Part display=parts.get(7+p.face.ordinal());
        return p.kind.reader()&&display!=null&&display.kind.panelDisplay();
    }
    public Part coveredReader(Part display){
        if(!display.kind.panelDisplay())return null;
        Part reader=parts.get(display.face.ordinal());return reader!=null&&reader.kind.reader()?reader:null;
    }
    public Part interactionTarget(BlockRayTraceResult hit,boolean behindDisplay){
        Part p=hit(hit);if(p!=null&&behindDisplay){Part reader=coveredReader(p);if(reader!=null)return reader;}return p;
    }
    public int connection(Direction face) { return cableConnections[face.ordinal()]; }
    int[] connections() { return cableConnections.clone(); }
    public void setConnections(int[] values) {
        if(!java.util.Arrays.equals(cableConnections,values)) { System.arraycopy(values,0,cableConnections,0,6); cachedOutline=null;lastCableSync=null; }
    }
    public net.minecraft.util.math.shapes.VoxelShape outline() {
        if(cachedOutline==null) {
            var shape=net.minecraft.util.math.shapes.VoxelShapes.empty();
            for(Part p:parts.values()){
                shape=net.minecraft.util.math.shapes.VoxelShapes.or(shape,MultipartShapes.part(parts.values(),p));
                if(externalLead(p))shape=net.minecraft.util.math.shapes.VoxelShapes.or(shape,MultipartShapes.lead(p));
            }
            if(parts.containsKey(6))for(Direction f:Direction.values())shape=net.minecraft.util.math.shapes.VoxelShapes.or(shape,PartShapes.arm(connection(f),f));
            cachedOutline=shape.isEmpty()?PartShapes.CENTRE:shape.optimize();
        }
        return cachedOutline;
    }
    public HostEntity(){super(FoundationsPL4.HOST_ENTITY.get());}
    public HostEntity(BlockPos p,BlockState s){this();setPosition(p);}
    @Override public void onLoad(){super.onLoad();if(level!=null){if(!level.isClientSide)NetworkEngine.add(this);else CableGeometry.refresh(this);}}
    @Override public void setRemoved(){if(level!=null&&!level.isClientSide)NetworkEngine.remove(this);super.setRemoved();if(level!=null&&level.isClientSide)CableGeometry.refresh(this);}
    public boolean canEdit(PlayerEntity p){return p.hasPermissions(2)||parts.values().stream().allMatch(a->a.owner==null||a.owner.equals(p.getUUID()));}
    public Part hit(BlockRayTraceResult h){
        Vector3d local=h.getLocation().subtract(Vector3d.atLowerCornerOf(worldPosition));
        // A front hit belongs to the thin display, never its covered reader; use real paired geometry.
        for(boolean display:new boolean[]{true,false})for(Part p:parts.values())if(!p.kind.cable()&&p.kind.display()==display)
            for(AxisAlignedBB b:MultipartShapes.part(parts.values(),p).toAabbs())if(b.inflate(.0001).contains(local))return p;
        for(Part p:parts.values())if(externalLead(p))for(AxisAlignedBB b:MultipartShapes.lead(p).toAabbs())if(b.inflate(.0001).contains(local))return p;
        return parts.getOrDefault(6,parts.values().stream().findFirst().orElse(null));
    }
    public Direction cableDirection(BlockRayTraceResult hit){
        Vector3d point=hit.getLocation().subtract(Vector3d.atCenterOf(worldPosition));
        double x=Math.abs(point.x),y=Math.abs(point.y),z=Math.abs(point.z);
        if(Math.max(x,Math.max(y,z))<=.126)return hit.getDirection();
        if(x>=y&&x>=z)return point.x<0?Direction.WEST:Direction.EAST;
        if(y>=z)return point.y<0?Direction.DOWN:Direction.UP;
        return point.z<0?Direction.NORTH:Direction.SOUTH;
    }
    public void changed(){
        cachedOutline=null;lastCableSync=null;setChanged();
        if(level!=null){
            if(!level.isClientSide){net.foundations.pl4.compat.PortCapabilities.invalidate(this);NetworkEngine.add(this);}
            NetworkEngine.invalidate(level);level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
            level.updateNeighborsAt(worldPosition,getBlockState().getBlock());
        }
    }
    public void syncIfChanged(){
        if(level==null||level.isClientSide)return;
        // Standalone cables have only status/signal/rows changing during sampling.
        // Persistent edits, geometry changes and reloads explicitly invalidate this shortcut.
        Part cable=parts.size()==1?parts.get(6):null;
        if(cable!=null&&cable.kind.cable()&&lastCableSync!=null&&lastCableSync.part()==cable
            &&lastCableSync.signal()==cable.signal&&lastCableSync.status().equals(cable.status)
            &&lastCableSync.rows().equals(cable.rows))return;
        syncTagBuilds++;
        CompoundNBT tag=getUpdateTag(level.registryAccess());
        if(!tag.equals(lastSync)){lastSync=tag;level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
        lastCableSync=cable!=null&&cable.kind.cable()?new CableSync(cable,cable.status,cable.signal,List.copyOf(cable.rows)):null;
    }
    public int output(Direction side){
        return parts.values().stream().filter(p->p.kind==Kind.SIGNALLER||p.kind==Kind.REDSTONE_RECEIVER||p.kind==Kind.CLOCK).mapToInt(p->p.signal).max().orElse(0);
    }
    protected void saveAdditional(CompoundNBT t,net.minecraft.util.registry.DynamicRegistries r){super.save(t);write(t,r,false);}
    private void write(CompoundNBT t,net.minecraft.util.registry.DynamicRegistries r,boolean sync){
        ListNBT list=new ListNBT();parts.values().forEach(p->list.add(p.save(r,sync)));t.put("parts",list);t.putInt("schema",2);
        if(sync){t.putIntArray("cableConnections",cableConnections);t.putInt("externalLeads",externalLeads);}
    }
    protected void loadAdditional(CompoundNBT t,net.minecraft.util.registry.DynamicRegistries r){
        cachedOutline=null;lastCableSync=null;java.util.Arrays.fill(cableConnections,0);
        int[] arms=t.getIntArray("cableConnections");if(arms.length==6)for(int a=0;a<6;a++)cableConnections[a]=net.foundations.pl4.compat.PortMath.clamp(arms[a],0,3);
        externalLeads=t.getInt("externalLeads")&8191;parts.clear();ListNBT list=t.getList("parts",net.minecraftforge.common.util.Constants.NBT.TAG_COMPOUND);
        // R6 stored kind+face, not a persisted slot key. Reindex displays without changing identity/layout.
        for(int i=0;i<Math.min(list.size(),net.foundations.pl4.core.MultipartTopology.SLOT_COUNT);i++){
            Part p=Part.load(list.getCompound(i),r);if(p!=null)parts.put(p.slot(),p);
        }
        if(level!=null){if(!level.isClientSide)NetworkEngine.invalidate(level);else CableGeometry.refresh(this);}
    }
    public CompoundNBT getUpdateTag(net.minecraft.util.registry.DynamicRegistries r){CompoundNBT t=new CompoundNBT();write(t,r,true);return t;}
    @Override public SUpdateTileEntityPacket getUpdatePacket(){return new SUpdateTileEntityPacket(getBlockPos(),0,getUpdateTag());}

    @Override public CompoundNBT save(CompoundNBT tag){saveAdditional(tag,null);return tag;}
    @Override public void load(BlockState state,CompoundNBT tag){super.load(state,tag);loadAdditional(tag,null);}
    @Override public CompoundNBT getUpdateTag(){CompoundNBT t=super.getUpdateTag();t.merge(getUpdateTag(null));return t;}
    @Override public void onDataPacket(net.minecraft.network.NetworkManager manager,SUpdateTileEntityPacket packet){load(getBlockState(),packet.getTag());}
}
