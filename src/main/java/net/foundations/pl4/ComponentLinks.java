package net.foundations.pl4;

import java.util.*;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/** Server-owned candidate selection; clients submit opaque IDs, never arbitrary target coordinates. */
public final class ComponentLinks {
    private ComponentLinks(){}
    public static boolean supported(Part part){return part.kind.receiver()||part.kind==Kind.ARRAY||part.kind==Kind.ENTITY_NODE;}
    private static List<Part.Link> candidates(ServerPlayer player,HostEntity host,Part part){
        List<Part.Link> links=new ArrayList<>();
        if(part.kind.receiver()){
            if(!PLConfig.WIRELESS.get())return links;
            for(var ref:NetworkEngine.all(player.getLevel().getServer())){
                if(!ref.part().kind.emitter()||ref.part().kind.redstone()!=part.kind.redstone()||!Objects.equals(ref.part().owner,part.owner)||!ref.host().canEdit(player)||!ref.level().mayInteract(player,ref.host().getBlockPos()))continue;
                if(!PLConfig.CROSS_DIMENSION.get()&&ref.level()!=host.getLevel())continue;
                links.add(new Part.Link(ref.level().dimension().location().toString(),ref.host().getBlockPos(),ref.part().face,null,ref.part().identity));
            }
        }else if(part.kind==Kind.ENTITY_NODE){
            double radius=Math.min(32,PLConfig.ENTITY_RANGE.get());
            for(var entity:player.getLevel().getEntitiesOfClass(LivingEntity.class,new AABB(host.getBlockPos()).inflate(radius),e->e.isAlive()&&!e.isSpectator()))
                if(player.getLevel().mayInteract(player,entity.blockPosition()))links.add(new Part.Link(player.level.dimension().location().toString(),entity.blockPosition(),Direction.UP,entity.getUUID(),null));
        }
        links.sort(Comparator.comparing(ComponentLinks::choiceId));return links;
    }
    public static void refresh(ServerPlayer player,HostEntity host,Part part){
        if(!supported(part))return;part.targetChoices.clear();
        List<Part.Link> choices=candidates(player,host,part).stream().filter(l->label(player,l).toLowerCase(Locale.ROOT).contains(part.targetQuery.toLowerCase(Locale.ROOT))).toList();
        part.targetCount=choices.size();part.targetPage=net.foundations.pl4.compat.PortMath.clamp(part.targetPage,0,Math.max(0,(choices.size()-1)/16));
        for(var link:choices.stream().skip(part.targetPage*16L).limit(16).toList())part.targetChoices.add(new Part.ReaderChoice(choiceId(link),label(player,link),"link"));
    }
    private static String choiceId(Part.Link link){return link.entity()==null?ReaderChannels.id(link):link.entity().toString();}
    private static String label(ServerPlayer player,Part.Link link){
        var level=NetworkEngine.level(player.getLevel().getServer(),link);String name="Emitter";
        if(link.entity()!=null){var entity=level==null?null:level.getEntity(link.entity());name=entity==null?"Missing entity":entity.getName().getString();}
        else if(level!=null&&level.hasChunkAt(link.pos())&&level.getBlockEntity(link.pos()) instanceof HostEntity h){for(var p:h.parts.values())if(p.identity.equals(link.part())&&!p.label.isBlank())name=p.label;}
        return ReaderChannels.clean(name+" · "+link.pos().toShortString()+" · "+link.dimension());
    }
    public static boolean addChoice(ServerPlayer player,HostEntity host,Part part,String id){
        for(var link:candidates(player,host,part))if(choiceId(link).equals(id))return add(player,host,part,link);return false;
    }
    public static boolean addHeld(ServerPlayer player,HostEntity host,Part part){
        for(InteractionHand hand:InteractionHand.values()){
            var stack=player.getItemInHand(hand);
            if(!stack.is(FoundationsPL4.item(part.kind==Kind.ENTITY_NODE?"entitytransceiver":"transceiver")))continue;
            if(add(player,host,part,ToolItem.link(stack)))return true;
        }return false;
    }
    static boolean add(ServerPlayer player,HostEntity host,Part part,Part.Link link){
        if(!supported(part)||link==null||!host.canEdit(player)||!host.getLevel().mayInteract(player,host.getBlockPos())||part.links.size()>=(part.kind.receiver()?64:8))return false;
        if(!PLConfig.CROSS_DIMENSION.get()&&!host.getLevel().dimension().location().toString().equals(link.dimension()))return false;
        var world=NetworkEngine.level(player.getLevel().getServer(),link);if(world==null)return false;
        if(part.kind.receiver()){
            if(!PLConfig.WIRELESS.get()||link.entity()!=null||link.part()==null||!world.hasChunkAt(link.pos())||!(world.getBlockEntity(link.pos()) instanceof HostEntity remote)||!remote.canEdit(player)||!world.mayInteract(player,link.pos()))return false;
            Part target=remote.parts.values().stream().filter(p->p.identity.equals(link.part())).findFirst().orElse(null);
            if(target==null||!target.kind.emitter()||target.kind.redstone()!=part.kind.redstone()||!Objects.equals(target.owner,part.owner))return false;
        }else if(part.kind==Kind.ENTITY_NODE){
            var entity=link.entity()==null?null:world.getEntity(link.entity());if(entity==null||!entity.isAlive()||!world.mayInteract(player,entity.blockPosition()))return false;
        }else if(link.entity()!=null||!world.hasChunkAt(link.pos())||!world.mayInteract(player,link.pos()))return false;
        boolean duplicate=part.links.stream().anyMatch(l->link.entity()!=null?link.entity().equals(l.entity())&&link.dimension().equals(l.dimension()):l.equals(link));
        if(duplicate)return false;part.links.add(link);return true;
    }
}
