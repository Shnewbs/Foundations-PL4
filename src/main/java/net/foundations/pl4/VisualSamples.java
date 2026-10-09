package net.foundations.pl4;

import java.io.*;
import java.security.MessageDigest;
import java.util.*;

import net.foundations.pl4.compat.DataComponents;
import net.minecraft.util.registry.Registry;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.INBT;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/** ITextComponent-aware aggregation with bounded visual snapshots. No per-slot serialization or client-side queries.
 * Nested container inventories and block entity NBT are not copied into public picture metadata. */
public final class VisualSamples {
    private record Key(Object type,Object patch) {}
    private static final class Sum {ItemStack item=ItemStack.EMPTY;FluidStack fluid=FluidStack.EMPTY;double value,capacity;}
    private final Map<Key,Sum> sums=new LinkedHashMap<>();
    public void item(ItemStack stack){
        ItemStack picture=net.foundations.pl4.compat.PortData.copyWithCount(stack,1);net.foundations.pl4.compat.PortData.remove(picture,DataComponents.CONTAINER);net.foundations.pl4.compat.PortData.remove(picture,DataComponents.BLOCK_ENTITY_DATA);
        Key key=new Key(picture.getItem(),net.foundations.pl4.compat.PortData.components(picture));Sum sum=sums.computeIfAbsent(key,k->{Sum s=new Sum();s.item=picture;return s;});sum.value+=stack.getCount();
    }
    public void fluid(FluidStack stack,int capacity){
        Key key=new Key(stack.getFluid(),net.foundations.pl4.compat.PortData.components(stack));Sum sum=sums.computeIfAbsent(key,k->{Sum s=new Sum();s.fluid=net.foundations.pl4.compat.PortData.copyWithAmount(stack,1);return s;});sum.value+=stack.getAmount();sum.capacity+=capacity;
    }
    public List<Part.Row> rows(net.minecraft.util.registry.DynamicRegistries registry,boolean descending,int limit,int offset){
        var sorted=new ArrayList<>(sums.values());Comparator<Sum> order=Comparator.comparingDouble(s->s.value);if(descending)order=order.reversed();sorted.sort(order);
        List<Part.Row> rows=new ArrayList<>();int budget=32768;
        for(int index=Math.max(0,offset);index<sorted.size();index++){Sum sum=sorted.get(index);if(rows.size()>=limit)break;
            ItemStack item=sum.item.copy();FluidStack fluid=sum.fluid.copy();String id,name,unit;CompoundNBT it=new CompoundNBT(),ft=new CompoundNBT();
            if(!item.isEmpty()){
                id=Registry.ITEM.getKey(item.getItem()).toString();name=item.getHoverName().getString();unit="items";
                // Rendering is allowed to retain custom models, dyes, enchantment glint and other bounded visual components.
                net.foundations.pl4.compat.PortData.remove(item,DataComponents.CONTAINER);net.foundations.pl4.compat.PortData.remove(item,DataComponents.BLOCK_ENTITY_DATA);
                it=(CompoundNBT)net.foundations.pl4.compat.PortData.save(item,registry);byte[] encoded=bounded(it,4096);
                String variant=encoded!=null&&!net.foundations.pl4.compat.PortData.components(sum.item).isEmpty()?fingerprint(it):"";
                if(encoded==null||encoded.length>budget){item=new ItemStack(item.getItem());it=(CompoundNBT)net.foundations.pl4.compat.PortData.save(item,registry);encoded=bounded(it,Math.min(4096,budget));}
                if(encoded==null)it=new CompoundNBT();
                budget=Math.max(0,budget-(encoded==null?0:encoded.length));
                if(!variant.isEmpty())id+="~"+variant;else if(!net.foundations.pl4.compat.PortData.components(sum.item).isEmpty()&&!it.isEmpty())id+="~"+fingerprint(it);
            }else{
                id=Registry.FLUID.getKey(fluid.getFluid()).toString();name=fluid.getDisplayName().getString();unit="mB";
                ft=(CompoundNBT)net.foundations.pl4.compat.PortData.save(fluid,registry);byte[] encoded=bounded(ft,4096);
                String variant=encoded!=null&&!net.foundations.pl4.compat.PortData.components(sum.fluid).isEmpty()?fingerprint(ft):"";
                if(encoded==null||encoded.length>budget){fluid=new FluidStack(fluid.getFluid(),1);ft=(CompoundNBT)net.foundations.pl4.compat.PortData.save(fluid,registry);encoded=bounded(ft,Math.min(4096,budget));}
                if(encoded==null)ft=new CompoundNBT();
                budget=Math.max(0,budget-(encoded==null?0:encoded.length));
                if(!variant.isEmpty())id+="~"+variant;else if(!net.foundations.pl4.compat.PortData.components(sum.fluid).isEmpty()&&!ft.isEmpty())id+="~"+fingerprint(ft);
            }
            rows.add(new Part.Row(id,cleanName(name),sum.value,sum.capacity,unit,item,fluid,it,ft));
        }return rows;
    }
    private static String cleanName(String s){return net.foundations.pl4.core.DisplayElements.clean(s,160);}
    /** Canonical, bounded preview identity: deterministic across save/reload and key iteration order. */
    private static String fingerprint(CompoundNBT tag){
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical(tag).getBytes(java.nio.charset.StandardCharsets.UTF_8))).substring(0,16);}catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
    private static String canonical(INBT tag){
        if(tag instanceof CompoundNBT compound){StringBuilder out=new StringBuilder("{");for(String key:new java.util.TreeSet<>(compound.getAllKeys()))out.append(key.length()).append(':').append(key).append('=').append(canonical(compound.get(key))).append(';');return out.append('}').toString();}
        if(tag instanceof ListNBT list){StringBuilder out=new StringBuilder("[");for(INBT entry:list)out.append(canonical(entry)).append(';');return out.append(']').toString();}return tag.toString();
    }
    public static byte[] bounded(CompoundNBT tag,int limit){
        try{var out=new ByteArrayOutputStream();OutputStream capped=new FilterOutputStream(out){int bytes;
            @Override public void write(int b)throws IOException{if(++bytes>limit)throw new IOException("Preview budget");out.write(b);}
            @Override public void write(byte[] b,int off,int len)throws IOException{if(bytes+len>limit)throw new IOException("Preview budget");bytes+=len;out.write(b,off,len);}
        };CompressedStreamTools.write(tag,new DataOutputStream(capped));return out.toByteArray();}catch(IOException ex){return null;}
    }
}
