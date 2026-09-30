package net.foundations.pl4;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.foundations.pl4.core.DisplayLayout;

/** Display joining never joins electrical/data cable networks. Only an explicit connected reader is sampled.
 * The active layout is mirrored to each member at rebuild/edit, so growth, root removal and reload are stable.
 */
public final class DisplayNetworks {
    private record Plane(ServerLevel level,UUID owner,Direction face,int depth,boolean outward) {}
    private record Canvas(NetworkEngine.Ref root,List<NetworkEngine.Ref> tiles,List<NetworkEngine.Ref> readers) {}
    private static final Map<Part,NetworkEngine.Ref> ROOTS=new IdentityHashMap<>();
    private static List<Canvas> canvases=List.of();
    public static void clear(){ROOTS.clear();canvases=List.of();}
    public static Direction right(Direction face){return switch(face){case SOUTH->Direction.WEST;case WEST->Direction.NORTH;case EAST->Direction.SOUTH;default->Direction.EAST;};}
    public static Direction up(Direction face){return switch(face){case DOWN->Direction.NORTH;case UP->Direction.SOUTH;default->Direction.UP;};}
    public static Direction right(Part part){return Direction.from3DDataValue(net.foundations.pl4.core.DisplayFacing.direction(net.foundations.pl4.core.DisplayFacing.frame(part.face.ordinal(),part.displayOutward).right()));}
    public static Direction up(Part part){return Direction.from3DDataValue(net.foundations.pl4.core.DisplayFacing.direction(net.foundations.pl4.core.DisplayFacing.frame(part.face.ordinal(),part.displayOutward).up()));}
    private static int dot(BlockPos p,Direction d){return p.getX()*d.getStepX()+p.getY()*d.getStepY()+p.getZ()*d.getStepZ();}
    public static void rebuild(List<NetworkEngine.Ref> refs,Map<Part,List<NetworkEngine.Ref>> visible){
        Set<Part> previousControllers=Collections.newSetFromMap(new IdentityHashMap<>());
        for(var canvas:canvases)previousControllers.add(canvas.root.part());
        clear();Map<Plane,Map<DisplayLayout.Cell,NetworkEngine.Ref>> planes=new HashMap<>();
        for(var r:refs)if(r.part().kind==Kind.LARGE_DISPLAY){
            Part p=r.part();p.canvasWidth=1;p.canvasHeight=1;p.canvasColumn=0;p.canvasRow=0;p.canvasMask=0;
            BlockPos pos=r.host().getBlockPos();
            Plane plane=new Plane(r.level(),p.owner,p.face,dot(pos,p.face),p.displayOutward);
            var cell=new DisplayLayout.Cell(dot(pos,right(p)),-dot(pos,up(p)));
            planes.computeIfAbsent(plane,k->new HashMap<>()).put(cell,r);
        }
        List<Canvas> result=new ArrayList<>();
        for(var plane:planes.values()){
            Set<DisplayLayout.Cell> seen=new HashSet<>();
            for(var start:plane.keySet()){
                if(!seen.add(start))continue;
                List<DisplayLayout.Cell> cells=new ArrayList<>();ArrayDeque<DisplayLayout.Cell> queue=new ArrayDeque<>();queue.add(start);
                while(!queue.isEmpty()){
                    var c=queue.removeFirst();cells.add(c);
                    for(var n:List.of(new DisplayLayout.Cell(c.x()-1,c.y()),new DisplayLayout.Cell(c.x()+1,c.y()),new DisplayLayout.Cell(c.x(),c.y()-1),new DisplayLayout.Cell(c.x(),c.y()+1)))
                        if(plane.containsKey(n)&&seen.add(n))queue.addLast(n);
                }
                var bounds=DisplayLayout.rectangle(cells);if(bounds.isEmpty())continue; // Nonrectangles stay independent.
                var rect=bounds.get();var root=plane.get(new DisplayLayout.Cell(rect.x(),rect.y()));
                List<NetworkEngine.Ref> tiles=new ArrayList<>();Set<Part> readerIds=Collections.newSetFromMap(new IdentityHashMap<>());List<NetworkEngine.Ref> readers=new ArrayList<>();
                for(var cell:cells){
                    var ref=plane.get(cell);Part part=ref.part();part.canvasWidth=rect.width();part.canvasHeight=rect.height();
                    part.canvasColumn=cell.x()-rect.x();part.canvasRow=cell.y()-rect.y();part.canvasMask=rect.mask(cell);
                    ROOTS.put(part,root);tiles.add(ref);
                    for(var candidate:visible.getOrDefault(part,List.of()))if(candidate.part().kind.reader()&&readerIds.add(candidate.part()))readers.add(candidate);
                }
                String donorId=net.foundations.pl4.core.CanvasContinuity.donor(tiles.stream().map(t->new net.foundations.pl4.core.CanvasContinuity.Candidate(
                    t.part().identity.toString(),t.part().layoutRevision,t.part().displaySettings().configured(),previousControllers.contains(t.part()),t.part()==root.part())).toList());
                Part donor=tiles.stream().map(NetworkEngine.Ref::part).filter(t->t.identity.toString().equals(donorId)).findFirst().orElseThrow();
                var settings=donor.displaySettings();long revision=donor.layoutRevision;
                var space=net.foundations.pl4.core.DynamicCanvasLayout.large(rect.width(),rect.height());
                int sourceWidth=settings.layoutWidth(),sourceHeight=settings.layoutHeight();
                int targetWidth=space.width(),targetHeight=space.height();
                if(sourceWidth!=targetWidth||sourceHeight!=targetHeight){
                    var migrated=settings.elements().stream().map(e->new Part.Element(net.foundations.pl4.core.DynamicCanvasLayout.migrate(e.spec(),sourceWidth,sourceHeight,targetWidth,targetHeight))).toList();
                    settings=new Part.DisplaySettings(settings.label(),settings.selected(),settings.metric(),settings.color(),migrated,settings.displayMode(),settings.displayPage(),targetWidth,targetHeight);
                    revision=net.foundations.pl4.core.CanvasContinuity.next(revision);
                }
                for(var tile:tiles)if(tile.part().applyDisplaySettings(settings,revision))tile.host().setChanged();
                readers.sort(readerOrder());
                result.add(new Canvas(root,List.copyOf(tiles),List.copyOf(readers)));
            }
        }
        // Nonrectangular large panels and ordinary/mini/holographic displays remain individually usable.
        for(var ref:refs)if(ref.part().kind.display()&&!ROOTS.containsKey(ref.part())){
            ROOTS.put(ref.part(),ref);
            List<NetworkEngine.Ref> readers=new ArrayList<>(visible.getOrDefault(ref.part(),List.of()));
            readers.sort(readerOrder());result.add(new Canvas(ref,List.of(ref),List.copyOf(readers)));
        }
        canvases=List.copyOf(result);
    }
    private static Comparator<NetworkEngine.Ref> readerOrder(){return Comparator.comparingInt((NetworkEngine.Ref r)->r.part().priority).reversed().thenComparing(r->r.level().dimension().location().toString()).thenComparingLong(r->r.host().getBlockPos().asLong()).thenComparingInt(r->r.part().slot());}
    /** Bounded preflight of the proposed connected plane; never requests an unloaded chunk. */
    public static boolean canExtendAt(net.minecraft.world.level.Level level,BlockPos position,Part template,net.minecraft.world.entity.player.Player player){
        Set<BlockPos> seen=new HashSet<>();ArrayDeque<BlockPos> queue=new ArrayDeque<>();queue.add(position);seen.add(position);
        List<DisplayLayout.Cell> cells=new ArrayList<>();Direction right=right(template),up=up(template);
        while(!queue.isEmpty()){
            BlockPos pos=queue.removeFirst();cells.add(new DisplayLayout.Cell(dot(pos,right),-dot(pos,up)));
            if(!net.foundations.pl4.core.DisplayPlacement.withinLimits(cells))return false;
            for(Direction side:List.of(right,right.getOpposite(),up,up.getOpposite())){
                BlockPos next=pos.relative(side);if(seen.contains(next)||!level.hasChunkAt(next))continue;
                if(level.getBlockEntity(next) instanceof HostEntity host){
                    Part part=host.parts.get(template.slot());
                    if(part!=null&&part.kind==Kind.LARGE_DISPLAY&&part.face==template.face&&part.displayOutward==template.displayOutward&&Objects.equals(part.owner,template.owner)){
                        if(!host.canEdit(player)||!level.mayInteract(player,next))return false;
                        seen.add(next);queue.addLast(next);
                    }
                }
            }
        }
        return true;
    }
    public static boolean canEditCanvas(net.minecraft.server.level.ServerPlayer player,HostEntity host,Part part){
        if(!host.canEdit(player)||!host.getLevel().mayInteract(player,host.getBlockPos()))return false;
        for(var canvas:canvases)if(canvas.tiles.stream().anyMatch(t->t.part()==part))
            for(var tile:canvas.tiles)if(!tile.host().canEdit(player)||!tile.level().mayInteract(player,tile.host().getBlockPos()))return false;
        return true;
    }
    /** Called after an authorized settings edit, before the topology is invalidated. */
    public static long nextLayoutRevision(Part part){
        long greatest=part.layoutRevision;
        for(var canvas:canvases)if(canvas.tiles.stream().anyMatch(t->t.part()==part))for(var tile:canvas.tiles)greatest=Math.max(greatest,tile.part().layoutRevision);
        return net.foundations.pl4.core.CanvasContinuity.next(greatest);
    }
    public static void applyLayout(HostEntity host,Part part,Part.DisplaySettings settings,long revision){
        part.applyDisplaySettings(settings,revision);
        for(var canvas:canvases)if(canvas.tiles.stream().anyMatch(t->t.part()==part))
            for(var tile:canvas.tiles)if(tile.part().applyDisplaySettings(settings,revision))tile.host().setChanged();
        host.setChanged();
    }
    public static void layoutEdited(HostEntity host,Part part){applyLayout(host,part,part.displaySettings(),nextLayoutRevision(part));}
    public static NetworkEngine.Ref controller(HostEntity host,Part part){
        if(host.getLevel() instanceof ServerLevel level)NetworkEngine.ensureCurrent(level.getServer());
        return ROOTS.getOrDefault(part,new NetworkEngine.Ref(host,part));
    }
    /** Preflight the possible merged front before any settings are mirrored into neighboring tiles. */
    private static boolean canFlipInto(net.minecraft.server.level.ServerPlayer player,List<NetworkEngine.Ref> tiles,boolean outward){
        if(tiles.getFirst().part().kind!=Kind.LARGE_DISPLAY)return true;
        Set<BlockPos> seen=new HashSet<>();ArrayDeque<BlockPos> queue=new ArrayDeque<>();
        for(var tile:tiles){seen.add(tile.host().getBlockPos());queue.add(tile.host().getBlockPos());}
        Part template=tiles.getFirst().part();var level=tiles.getFirst().level();Direction right=right(template),up=up(template);
        while(!queue.isEmpty()){
            BlockPos pos=queue.removeFirst();
            for(Direction side:List.of(right,right.getOpposite(),up,up.getOpposite())){
                BlockPos next=pos.relative(side);if(seen.contains(next)||!level.hasChunkAt(next))continue;
                if(level.getBlockEntity(next) instanceof HostEntity host){
                    Part part=host.parts.get(template.slot());
                    if(part!=null&&part.kind==Kind.LARGE_DISPLAY&&part.face==template.face&&part.displayOutward==outward&&Objects.equals(part.owner,template.owner)){
                        if(!host.canEdit(player)||!level.mayInteract(player,next))return false;
                        seen.add(next);queue.addLast(next);
                        // Refuse oversized/protection scans, never force-load an unbounded plane.
                        if(seen.size()>256)return false;
                    }
                }
            }
        }
        return true;
    }
    /** Explicit whole-canvas flip retains a snapshot of the active settings, even if a new donor appears. */
    public static boolean flip(net.minecraft.server.level.ServerPlayer player,HostEntity anchor,Part part,boolean outward){
        NetworkEngine.ensureCurrent(player.getServer());
        var root=controller(anchor,part);
        List<NetworkEngine.Ref> tiles=List.of(new NetworkEngine.Ref(anchor,part));
        for(var canvas:canvases)if(canvas.tiles.stream().anyMatch(t->t.part()==part)){tiles=canvas.tiles;break;}
        for(var tile:tiles)if(!tile.host().canEdit(player)||!tile.level().mayInteract(player,tile.host().getBlockPos()))return false;
        if(!canFlipInto(player,tiles,outward))return false;
        var settings=root.part().displaySettings();long revision=root.part().layoutRevision;
        for(var tile:tiles){tile.part().displayOutward=outward;tile.host().setChanged();}
        NetworkEngine.invalidate(anchor.getLevel());NetworkEngine.ensureCurrent(player.getServer());
        var newRoot=controller(anchor,part);newRoot.part().applyDisplaySettings(settings,revision);
        if(newRoot.part().kind==Kind.LARGE_DISPLAY)layoutEdited(newRoot.host(),newRoot.part());
        newRoot.host().changed();for(var tile:tiles)tile.host().changed();
        return true;
    }
    private static NetworkEngine.Ref reader(List<NetworkEngine.Ref> readers,String selector){
        if(selector.isEmpty())return readers.isEmpty()?null:readers.getFirst();
        var exact=readers.stream().filter(r->r.part().identity.toString().equals(selector)).findFirst();if(exact.isPresent())return exact.get();
        var named=readers.stream().filter(r->r.part().label.equals(selector)).limit(2).toList();return named.size()==1?named.getFirst():null;
    }
    public static List<Part.Row> preview(HostEntity host,Part part,String selector){
        if(host.getLevel() instanceof ServerLevel level)NetworkEngine.ensureCurrent(level.getServer());
        for(var canvas:canvases)if(canvas.tiles.stream().anyMatch(t->t.part()==part)){
            var ref=reader(canvas.readers,selector);return ref==null?List.of():List.copyOf(ref.part().rows.subList(0,Math.min(64,ref.part().rows.size())));
        }return List.of();
    }
    public static void sample(){
        for(var canvas:canvases){
            Part root=canvas.root.part();var source=reader(canvas.readers,root.selected);
            root.rows.clear();if(source!=null)root.rows.addAll(source.part().rows);
            root.status=source==null?"No unique selected reader":source.part().title();
            root.readerChoices.clear();for(var option:canvas.readers.stream().limit(64).toList())root.readerChoices.add(new Part.ReaderChoice(option.part().identity.toString(),option.part().title(),option.part().kind.id));
            root.sourceRows.clear();int budget=256;
            // Only already-visible readers may satisfy bindings. Never resolve a global reader name/UUID.
            for(var element:root.elements){
                String selector=element.reader();if(selector.isBlank()||root.sourceRows.containsKey(selector)||root.sourceRows.size()>=8)continue;
                var bound=reader(canvas.readers,selector);List<Part.Row> values=bound==null?List.of():List.copyOf(bound.part().rows.subList(0,Math.min(Math.min(64,budget),bound.part().rows.size())));
                root.sourceRows.put(selector,values);budget-=values.size();
            }
            if(canvas.tiles.size()>1)root.status+=" ("+root.canvasWidth+" x "+root.canvasHeight+")";
        }
    }
    private DisplayNetworks(){}
}
