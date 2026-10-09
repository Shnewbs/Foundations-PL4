package net.foundations.pl4;

import java.util.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraftforge.fluids.FluidStack;

/** Independent receive/send filters. INHERIT preserves the old Transfer Node filter. */
public final class TransferFilters {
    public static final Set<String> MODES=Set.of("INHERIT","ALLOW","DENY");
    private record Rule(String text,Set<String> ids,List<TagKey<Item>> items,List<TagKey<net.minecraft.world.level.material.Fluid>> fluids){}
    private static final Map<Part,Rule[]> CACHE=new WeakHashMap<>();
    private static Rule rule(Part p,boolean input){
        String mode=input?p.inputFilterMode:p.outputFilterMode;
        String text=mode.equals("INHERIT")?(p.kind==Kind.TRANSFER_NODE?p.filter:""):(input?p.inputFilter:p.outputFilter);
        Rule[] pair=CACHE.computeIfAbsent(p,unused->new Rule[2]);int index=input?0:1;
        if(pair[index]!=null&&pair[index].text().equals(text))return pair[index];
        Set<String> ids=new HashSet<>();List<TagKey<Item>> items=new ArrayList<>();List<TagKey<net.minecraft.world.level.material.Fluid>> fluids=new ArrayList<>();
        for(String token:text.split(",")){String entry=token.trim();if(entry.startsWith("#")){ResourceLocation id=ResourceLocation.tryParse(entry.substring(1));if(id!=null){items.add(TagKey.create(Registries.ITEM,id));fluids.add(TagKey.create(Registries.FLUID,id));}}else ids.add(entry);}
        return pair[index]=new Rule(text,Set.copyOf(ids),List.copyOf(items),List.copyOf(fluids));
    }
    private static boolean allowed(Part p,boolean input,Rule rule,boolean match){
        if(rule.text().isBlank())return true;
        String mode=input?p.inputFilterMode:p.outputFilterMode;
        return match==(mode.equals("INHERIT")?p.whitelist:mode.equals("ALLOW"));
    }
    public static boolean items(ItemStack stack,Part p,boolean input){
        if(p.kind==Kind.TRANSFER_NODE&&!p.items)return false;
        Rule rule=rule(p,input);boolean match=rule.ids().contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        if(!match)for(var tag:rule.items())if(stack.is(tag)){match=true;break;}
        return allowed(p,input,rule,match);
    }
    public static boolean fluids(FluidStack stack,Part p,boolean input){
        if(p.kind==Kind.TRANSFER_NODE&&!p.fluids)return false;
        Rule rule=rule(p,input);boolean match=rule.ids().contains(BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString());
        if(!match)for(var tag:rule.fluids())if(stack.getFluid().is(tag)){match=true;break;}
        return allowed(p,input,rule,match);
    }
    public static String mode(String mode){return MODES.contains(mode)?mode:"INHERIT";}
    private TransferFilters(){}
}
