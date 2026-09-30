package net.foundations.pl4;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.foundations.pl4.core.MultipartTopology;

/** Bounded local geometry, independent of sampling and server network membership.
 * Reconcile both cable ends from available parts, rather than separately arriving arm snapshots.
 * Only existing loaded hosts are inspected; no client placement or chunk loading is predicted. */
public final class CableGeometry {
    private static final Direction[] FACES=Direction.values();
    public static void refresh(HostEntity anchor){
        Level level=anchor.getLevel();if(level==null)return;
        Map<BlockPos,HostEntity> centres=new LinkedHashMap<>();
        add(level,anchor.getBlockPos(),centres);
        for(Direction face:FACES)add(level,anchor.getBlockPos().relative(face),centres);
        // Every centre needs all six neighbours. The two-cell footprint is bounded to 25 cells.
        Map<BlockPos,HostEntity> footprint=new LinkedHashMap<>(centres);
        for(HostEntity centre:centres.values())for(Direction face:FACES)add(level,centre.getBlockPos().relative(face),footprint);
        List<MultipartTopology.Node> nodes=new ArrayList<>();Map<Part,Integer> ids=new IdentityHashMap<>();
        String dimension=level.dimension().identifier().toString();
        for(HostEntity host:footprint.values())for(Part part:host.parts.values()){
            int id=nodes.size();ids.put(part,id);BlockPos pos=host.getBlockPos();
            nodes.add(new MultipartTopology.Node(id,new MultipartTopology.Cell(dimension,pos.getX(),pos.getY(),pos.getZ()),part.kind,part.face.ordinal(),part.blockedFaces));
        }
        var plan=MultipartTopology.plan(nodes);
        for(HostEntity host:centres.values()){
            Part cable=host.parts.get(6);host.setConnections(cable!=null&&cable.kind.cable()?plan.cableArms().get(ids.get(cable)):new int[6]);
            int leads=0;for(Part part:host.parts.values())if(plan.externalLeads().contains(ids.get(part)))leads|=1<<part.slot();
            host.setExternalLeads(leads);
            if(!level.isClientSide)host.syncIfChanged();
        }
    }
    private static void add(Level level,BlockPos pos,Map<BlockPos,HostEntity> hosts){
        if(!hosts.containsKey(pos)&&level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof HostEntity host&&!host.isRemoved())hosts.put(pos,host);
    }
    private CableGeometry(){}
}
