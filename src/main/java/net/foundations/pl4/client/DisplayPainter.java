package net.foundations.pl4.client;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.foundations.pl4.Part;
import net.foundations.pl4.core.DisplayElements;
import net.foundations.pl4.core.MonitorPresentation;

/** Same typed scene is used on real monitors and while editing. There is no inventory-list fallback for an invalid element. */
final class DisplayPainter {
    private record Batch(List<Part.Row> rows,DisplayElements.Scene scene){}
    private record Compiled(List<Part.Element> elements,List<Part.Row> rows,Map<String,List<Part.Row>> sources,int page,List<Batch> batches){}
    private final Map<Part,Compiled> cache=new WeakHashMap<>(); // Values never retain Part/Host/Level.
    void paint(Part part,List<Part.Element> elements,DisplayCanvas canvas,MonitorPresentation.Region area,boolean editing){
        if(MonitorPresentation.automatic(part.displayMode,editing)&&elements.equals(part.elements)){
            automatic(part,canvas,area);return;
        }
        // Editing previews the actual CUSTOM scene even when the saved resting view is AUTO_LIST.
        // No mode or world data is mutated by opening the editor.
        Compiled data=cache.get(part);
        if(data==null||data.page!=part.displayPage||!data.elements.equals(elements)||!data.rows.equals(part.rows)||!data.sources.equals(part.sourceRows)){
            List<Batch> batches=new ArrayList<>();for(var element:elements){var spec=element.spec();if(spec.page()!=part.displayPage||spec.options().hidden())continue;List<Part.Row> rows=source(part,spec);
                batches.add(new Batch(rows,DisplayElements.plan(spec,rows)));}
            data=new Compiled(List.copyOf(elements),List.copyOf(part.rows),Map.copyOf(part.sourceRows),part.displayPage,List.copyOf(batches));cache.put(part,data);
        }
        int icons=0,order=0;
        render: for(var batch:data.batches){canvas.order(order++);for(var draw:batch.scene.draws()){
            if(draw instanceof DisplayElements.Box box){var r=box.rect();if(box.filled())canvas.rect(r.x(),r.y(),r.width(),r.height(),box.color(),box.layer());else canvas.outline(r,box.color(),box.layer());}
            else if(draw instanceof DisplayElements.Text text)canvas.text(text.value(),text.x(),text.y(),text.width(),text.height(),text.color(),text.alignment(),text.wrap(),text.scale(),text.overlay()?4:3);
            else if(draw instanceof DisplayElements.Icon icon){if(icons++>=DisplayElements.MAX_ICONS)break render;canvas.item(batch.rows.get(icon.sample()),icon.rect(),icon.block());}
            else if(draw instanceof DisplayElements.Liquid fluid){if(icons++>=DisplayElements.MAX_ICONS)break render;canvas.fluid(batch.rows.get(fluid.sample()),fluid.rect(),fluid.fraction());}
        }
        }
        canvas.order(0);
        if(icons>DisplayElements.MAX_ICONS)canvas.text("128-picture budget; use another page",2,110,244,0xF2A7A7,false,4);
    }
    private void automatic(Part part,DisplayCanvas canvas,MonitorPresentation.Region area){
        var font=Minecraft.getInstance().font;var table=MonitorPresentation.table(area,part.rows.size());
        canvas.order(0);
        canvas.text(MonitorPresentation.title(part.label,part.status),table.left(),table.titleY(),table.contentWidth(),part.color,false,3);
        canvas.rect(table.left(),table.ruleY(),table.contentWidth(),.5,0xFF637A7E,1);
        canvas.text("NAME",table.left(),table.titleY()+18,Math.max(1,table.contentWidth()/2),0x86999C,false,3);
        canvas.text("AMOUNT",table.left(),table.titleY()+18,table.contentWidth(),0x86999C,true,3);
        for(int i=0;i<table.rows();i++){
            var row=part.rows.get(i);int y=table.rowY(i);
            String value=MonitorPresentation.amount(row);
            int valueWidth=Math.min(font.width(value),Math.max(20,table.contentWidth()/2));
            canvas.text(row.name(),table.left(),y,Math.max(1,table.contentWidth()-valueWidth-10),part.color,false,3);
            canvas.text(value,table.right()-valueWidth,y,valueWidth,0xE5ECEB,true,3);
        }
        if(part.rows.isEmpty())canvas.text(part.status.isBlank()?"Connect a reader; check its Data tab.":part.status,table.left(),table.firstRowY(),table.contentWidth(),0xA4B5B9,false,3);
        canvas.rect(table.left(),table.footerY()-4,table.contentWidth(),.5,0xFF465456,1);
        canvas.text(MonitorPresentation.footer(table.rows(),part.rows.size()),table.left(),table.footerY(),table.contentWidth(),0x86999C,false,3);
    }
    private static List<Part.Row> source(Part part,DisplayElements.Spec spec){
        if(!spec.asset().isBlank()){
            var id=ResourceLocation.tryParse(spec.asset());if(id==null)return List.of();
            if(spec.type()==DisplayElements.Type.FLUID||spec.type()==DisplayElements.Type.FLUID_GRID){
                var value=BuiltInRegistries.FLUID.get(id);if(value==net.minecraft.world.level.material.Fluids.EMPTY)return List.of();FluidStack fluid=new FluidStack(value,1);
                return List.of(new Part.Row(spec.asset(),fluid.getHoverName().getString(),1,1,"mB",ItemStack.EMPTY,fluid,new CompoundTag(),new CompoundTag()));
            }
            ItemStack item=new ItemStack(BuiltInRegistries.ITEM.get(id));if(item.isEmpty())return List.of();
            return List.of(new Part.Row(spec.asset(),item.getHoverName().getString(),1,0,"items",item,FluidStack.EMPTY,new CompoundTag(),new CompoundTag()));
        }
        return spec.reader().isBlank()?List.copyOf(part.rows):part.sourceRows.getOrDefault(spec.reader(),List.of());
    }
}
