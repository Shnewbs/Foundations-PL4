package net.foundations.pl4;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.CropBlock;
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
        Part p=ref.part();VisualSamples pictures=new VisualSamples();if(p.kind==Kind.ENERGY_READER)return EnergyReader.sample(server,ref,targets);Map<String,Part.Row> rows=new LinkedHashMap<>();double total=0,capacity=0;int available=0;
        if(p.kind==Kind.NETWORK_READER)return List.of(new Part.Row("hosts","Hosts",hosts,0,""),new Part.Row("targets","Targets",targets.size(),0,""));
        for(Part.Link link:targets){
            ServerLevel l=NetworkEngine.level(server,link);if(l==null)continue;
            if(link.entity()!=null){Entity entity=l.getEntity(link.entity());if(entity!=null&&p.kind==Kind.INFO_READER)entity(rows,entity);continue;}
            if(!l.hasChunkAt(link.pos()))continue;available++;
            if(p.kind==Kind.INVENTORY_READER){
                var handler=l.getCapability(Capabilities.ItemHandler.BLOCK,link.pos(),link.side());if(handler==null)continue;
                for(int slot=0;slot<Math.min(handler.getSlots(),65536);slot++){
                    ItemStack stack=handler.getStackInSlot(slot);capacity+=handler.getSlotLimit(slot);total+=stack.getCount();
                    if(stack.isEmpty()||!matches(stack,p)||p.mode.equals("SLOT")&&slot!=p.index)continue;
                    pictures.item(stack);
                }
            }else if(p.kind==Kind.FLUID_READER){
                var handler=l.getCapability(Capabilities.FluidHandler.BLOCK,link.pos(),link.side());if(handler==null)continue;
                for(int tank=0;tank<Math.min(handler.getTanks(),65536);tank++){
                    FluidStack stack=handler.getFluidInTank(tank);capacity+=handler.getTankCapacity(tank);total+=stack.getAmount();
                    if(stack.isEmpty()||!matches(stack,p))continue;
                    pictures.fluid(stack,handler.getTankCapacity(tank));
                }
            }else if(p.kind==Kind.INFO_READER){
                info(rows,l,link);break; // PL2 info reader chooses one channel at a time.
            }
        }
        if(p.kind==Kind.INVENTORY_READER||p.kind==Kind.FLUID_READER){
            for(Part.Row row:pictures.rows(ref.level().registryAccess(),p.descending,p.mode.equals("POS")?1:PLConfig.MAX_ROWS.get(),p.mode.equals("POS")?p.index:0))merge(rows,row);
        }
        if(p.mode.equals("STORAGE")){
            String unit=p.kind==Kind.FLUID_READER?"mB":"items";
            if(p.mode.equals("STORAGE"))rows.clear();
            LinkedHashMap<String,Part.Row> withTotal=new LinkedHashMap<>();withTotal.put("storage",new Part.Row("storage","Storage",total,capacity,unit));withTotal.putAll(rows);rows=withTotal;
        }
        List<Part.Row> result=new ArrayList<>(rows.values());
        if(p.kind==Kind.INVENTORY_READER||p.kind==Kind.FLUID_READER){
            Comparator<Part.Row> order=Comparator.comparingDouble(Part.Row::value);if(p.descending)order=order.reversed();result.sort(order.thenComparing(Part.Row::key));
            // POS has been applied to aggregated variants before serializing pictures.
            if(p.mode.equals("STACK")&&!p.metric.isBlank())result.removeIf(r->!r.key().equals(p.metric)&&!r.itemId().equals(p.metric)&&!r.fluidId().equals(p.metric));
        }
        if(p.kind==Kind.INFO_READER&&!p.metric.isBlank()){Set<String> keys=new HashSet<>(Arrays.asList(p.metric.split(",")));result.removeIf(r->!keys.contains(r.key()));}
        return result.subList(0,Math.min(result.size(),PLConfig.MAX_ROWS.get()));
    }
    private static void merge(Map<String,Part.Row> rows,Part.Row row){Part.Row old=rows.get(row.key());rows.put(row.key(),old==null?row:new Part.Row(row.key(),row.name(),old.value()+row.value(),old.capacity()+row.capacity(),row.unit(),row.item(),row.fluid(),row.previewItem(),row.previewFluid()));}
    private static void put(Map<String,Part.Row> rows,String key,String name,double value,double capacity,String unit){rows.put(key,new Part.Row(key,name,value,capacity,unit));}
    private static void info(Map<String,Part.Row> rows,ServerLevel l,Part.Link link){
        BlockPos pos=link.pos();var state=l.getBlockState(pos);
        put(rows,"x","X",pos.getX(),0,"");put(rows,"y","Y",pos.getY(),0,"");put(rows,"z","Z",pos.getZ(),0,"");
        put(rows,"redstone","Redstone",l.getBestNeighborSignal(pos),15,"");
        put(rows,"light","Block light",l.getBrightness(LightLayer.BLOCK,pos),15,"");put(rows,"sky_light","Sky light",l.getBrightness(LightLayer.SKY,pos),15,"");
        put(rows,"rain","Raining",l.isRaining()?1:0,1,"");put(rows,"thunder","Thundering",l.isThundering()?1:0,1,"");
        put(rows,"day_time","Day time",l.getDayTime()%24000,24000,"ticks");put(rows,"hardness","Hardness",state.getDestroySpeed(l,pos),0,"");
        if(state.getBlock() instanceof CropBlock crop)put(rows,"crop_age","Crop growth",crop.getAge(state),crop.getMaxAge(),"");
        for(var property:state.getProperties())if(state.getValue(property) instanceof Number number)put(rows,"state."+property.getName(),property.getName(),number.doubleValue(),0,"");
        if(l.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity furnace){
            var tag=furnace.saveWithoutMetadata(l.registryAccess());
            put(rows,"burn_time","Burn time",tag.getShort("BurnTime"),0,"ticks");put(rows,"cook_time","Cooking",tag.getShort("CookTime"),tag.getShort("CookTimeTotal"),"ticks");
        }
    }
    private static void entity(Map<String,Part.Row> rows,Entity e){
        put(rows,"x","X",e.getX(),0,"");put(rows,"y","Y",e.getY(),0,"");put(rows,"z","Z",e.getZ(),0,"");
        put(rows,"speed","Speed",e.getDeltaMovement().length()*20,0,"blocks/s");
        if(e instanceof LivingEntity living){put(rows,"health","Health",living.getHealth(),living.getMaxHealth(),"HP");put(rows,"armor","Armor",living.getArmorValue(),20,"");}
        if(e instanceof Player player){put(rows,"food","Hunger",player.getFoodData().getFoodLevel(),20,"");put(rows,"saturation","Saturation",player.getFoodData().getSaturationLevel(),20,"");put(rows,"xp","XP level",player.experienceLevel,0,"");}
    }
}
