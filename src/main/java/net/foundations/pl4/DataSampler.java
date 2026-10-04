package net.foundations.pl4;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;

public final class DataSampler {
    private record ParsedFilter(String source,Set<String> ids,
        List<TagKey<Item>> itemTags,List<TagKey<net.minecraft.world.level.material.Fluid>> fluidTags) {}
    // Server-thread cache. Values never retain their weak Part keys or world/capability objects.
    private static final Map<Part,ParsedFilter> FILTERS=new WeakHashMap<>();
    private static long filterCompiles;
    static long filterCompileCount(){return filterCompiles;}
    static void clearFilters(){FILTERS.clear();filterCompiles=0;}
    private static ParsedFilter filter(Part part){
        ParsedFilter cached=FILTERS.get(part);
        if(cached!=null&&cached.source().equals(part.filter))return cached;
        Set<String> ids=new HashSet<>();List<TagKey<Item>> items=new ArrayList<>();
        List<TagKey<net.minecraft.world.level.material.Fluid>> fluids=new ArrayList<>();
        for(String token:part.filter.split(",")){
            String entry=token.trim();
            if(entry.startsWith("#")){
                Identifier id=Identifier.tryParse(entry.substring(1));
                if(id!=null){items.add(TagKey.create(Registries.ITEM,id));fluids.add(TagKey.create(Registries.FLUID,id));}
            }else ids.add(entry); // Preserve exact namespaced-ID semantics, including invalid IDs.
        }
        ParsedFilter compiled=new ParsedFilter(part.filter,Set.copyOf(ids),List.copyOf(items),List.copyOf(fluids));
        FILTERS.put(part,compiled);filterCompiles++;return compiled;
    }
    public static boolean matches(ItemStack stack,Part part){
        if(part.filter.isBlank())return true;
        ParsedFilter filter=filter(part);boolean match=filter.ids().contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        if(!match)for(var tag:filter.itemTags())if(stack.is(tag)){match=true;break;}
        return match==part.whitelist;
    }
    public static boolean matches(FluidStack stack,Part part){
        if(part.filter.isBlank())return true;
        ParsedFilter filter=filter(part);boolean match=filter.ids().contains(BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString());
        if(!match)for(var tag:filter.fluidTags())if(stack.getFluid().is(tag)){match=true;break;}
        return match==part.whitelist;
    }
    public static List<Part.Row> sample(MinecraftServer server,NetworkEngine.Ref ref,List<Part.Link> targets,int hosts){
        Part p=ref.part();VisualSamples pictures=new VisualSamples();if(p.kind==Kind.ENERGY_READER)return EnergyReader.sample(server,ref,targets);Map<String,Part.Row> rows=new LinkedHashMap<>();double total=0,capacity=0;SampleSources sources=new SampleSources();
        if(p.kind==Kind.NETWORK_READER){
            Set<Part.Link> unique=new LinkedHashSet<>(targets);int loaded=0,entities=0;
            for(var target:unique){if(ReaderChannels.available(server,target))loaded++;if(target.entity()!=null)entities++;}
            return List.of(new Part.Row("hosts","Hosts",hosts,0,""),new Part.Row("targets","Targets",unique.size(),0,""),
                new Part.Row("available","Available targets",loaded,unique.size(),""),new Part.Row("unavailable","Unloaded / missing targets",unique.size()-loaded,unique.size(),""),
                new Part.Row("block_targets","Block endpoints",unique.size()-entities,0,""),new Part.Row("entity_targets","Entity endpoints",entities,0,"")).stream().limit(PLConfig.MAX_ROWS.get()).toList();
        }
        for(Part.Link link:new LinkedHashSet<>(targets)){
            ServerLevel l=NetworkEngine.level(server,link);if(l==null)continue;
            if(link.entity()!=null){Entity entity=l.getEntity(link.entity());if(entity!=null&&p.kind==Kind.INFO_READER){for(var row:net.foundations.pl4.api.InfoProviders.sample(l,link))rows.put(row.key(),row);break;}continue;}
            if(!l.hasChunkAt(link.pos()))continue;
            if(p.kind==Kind.INVENTORY_READER){
                var handler=net.foundations.pl4.compat.TransferAdapters.items(l,link.pos(),link.side());if(handler==null)continue;
                if(!sources.inventory(l,link,handler.identity(),handler.getSlots()))continue;
                for(int slot=0;slot<Math.min(handler.getSlots(),65536);slot++){
                    ItemStack stack=handler.getStackInSlot(slot);capacity+=handler.getSlotLimit(slot);total+=stack.getCount();
                    if(stack.isEmpty()||!matches(stack,p)||p.mode.equals("SLOT")&&slot!=p.index)continue;
                    pictures.item(stack);
                }
            }else if(p.kind==Kind.FLUID_READER){
                var handler=net.foundations.pl4.compat.TransferAdapters.fluids(l,link.pos(),link.side());if(handler==null)continue;
                if(!sources.fluid(handler.identity()))continue;
                for(int tank=0;tank<Math.min(handler.getTanks(),65536);tank++){
                    FluidStack stack=handler.getFluidInTank(tank);capacity+=handler.getTankCapacity(tank);total+=stack.getAmount();
                    if(stack.isEmpty()||!matches(stack,p)||p.mode.equals("SLOT")&&tank!=p.index)continue;
                    pictures.fluid(stack,handler.getTankCapacity(tank));
                }
            }else if(p.kind==Kind.INFO_READER){
                for(var row:net.foundations.pl4.api.InfoProviders.sample(l,link))rows.put(row.key(),row);break; // PL2 info reader chooses one channel at a time.
            }
        }
        if(p.kind==Kind.INVENTORY_READER||p.kind==Kind.FLUID_READER){
            for(Part.Row row:pictures.rows(ref.level().registryAccess(),p.descending,p.mode.equals("POS")?1:PLConfig.MAX_ROWS.get(),p.mode.equals("POS")?p.index:0))merge(rows,row);
        }
        if(p.mode.equals("STORAGE")&&(p.kind==Kind.INVENTORY_READER||p.kind==Kind.FLUID_READER)){
            String unit=p.kind==Kind.FLUID_READER?"mB":"items";
            rows.clear();
            LinkedHashMap<String,Part.Row> withTotal=new LinkedHashMap<>();withTotal.put("storage",new Part.Row("storage","Storage",total,capacity,unit));withTotal.putAll(rows);rows=withTotal;
        }
        List<Part.Row> result=new ArrayList<>(rows.values());
        if(p.kind==Kind.INVENTORY_READER||p.kind==Kind.FLUID_READER){
            Comparator<Part.Row> order=Comparator.comparingDouble(Part.Row::value);if(p.descending)order=order.reversed();result.sort(order.thenComparing(Part.Row::key));
            // POS has been applied to aggregated variants before serializing pictures.
            if(p.mode.equals("STACK")&&!p.metric.isBlank())result.removeIf(r->!r.key().equals(p.metric)&&!r.itemId().equals(p.metric)&&!r.fluidId().equals(p.metric));
        }
        if(p.kind==Kind.INFO_READER&&!p.metric.isBlank()){Set<String> keys=new HashSet<>();for(String key:p.metric.split(","))keys.add(key.trim());result.removeIf(r->!keys.contains(r.key()));}
        return result.subList(0,Math.min(result.size(),PLConfig.MAX_ROWS.get()));
    }
    private static void merge(Map<String,Part.Row> rows,Part.Row row){Part.Row old=rows.get(row.key());rows.put(row.key(),old==null?row:new Part.Row(row.key(),row.name(),old.value()+row.value(),old.capacity()+row.capacity(),row.unit(),row.item(),row.fluid(),row.previewItem(),row.previewFluid()));}
}
