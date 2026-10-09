package net.foundations.pl4;

import com.google.gson.*;
import java.util.*;
import net.foundations.pl4.core.DisplayElements;
import net.foundations.pl4.core.LayoutTransactions;

/** Versioned, bounded layout document. Contains data only; no paths or executable objects. */
public record LayoutTemplate(int width,int height,List<DisplayElements.Spec> elements) {
    public static final int MAX_TEXT=65536;
    public LayoutTemplate {
        if(width<8||height<9||width>DisplayElements.MAX_CANVAS||height>DisplayElements.MAX_CANVAS)throw new IllegalArgumentException("Invalid template canvas");
        elements=List.copyOf(elements);
        var before=new LayoutTransactions.State(List.of(),DisplayElements.Mode.CUSTOM,0,0);
        var result=LayoutTransactions.applyReplace(before,0,elements);
        if(!result.accepted())throw new IllegalArgumentException(result.message());
        for(var e:elements)if(e.bounds().right()>width||e.bounds().bottom()>height)throw new IllegalArgumentException("Element outside template canvas");
    }
    public String encode(){
        JsonObject j=new JsonObject();j.addProperty("format","foundations_pl4:layout");j.addProperty("schema",1);j.addProperty("width",width);j.addProperty("height",height);j.add("elements",new JsonParser().parse(ElementJson.encodeList(elements)));
        String text=j.toString();if(text.length()>MAX_TEXT)throw new IllegalArgumentException("Template too large");return text;
    }
    public static LayoutTemplate decode(String text){
        if(text==null||text.length()>MAX_TEXT)throw new IllegalArgumentException("Template too large");
        JsonObject j=new JsonParser().parse(text).getAsJsonObject();
        if(!j.get("format").getAsString().equals("foundations_pl4:layout")||j.get("schema").getAsBigDecimal().intValueExact()!=1)throw new IllegalArgumentException("Unsupported template format/schema");
        int w=j.get("width").getAsBigDecimal().intValueExact(),h=j.get("height").getAsBigDecimal().intValueExact();
        for(var value:j.getAsJsonArray("elements")){
            JsonObject e=value.getAsJsonObject();int x=e.get("x").getAsBigDecimal().intValueExact(),y=e.get("y").getAsBigDecimal().intValueExact(),ew=e.get("w").getAsBigDecimal().intValueExact(),eh=e.get("h").getAsBigDecimal().intValueExact();
            if(x<0||y<0||ew<8||eh<9||(long)x+ew>w||(long)y+eh>h)throw new IllegalArgumentException("Invalid template element bounds");
        }
        return new LayoutTemplate(w,h,ElementJson.decodeList(j.get("elements").toString()));
    }
    public static LayoutTemplate preset(int preset,int width,int height){
        if(width<64||height<48)throw new IllegalArgumentException("Starter boards need at least 64 x 48 pixels");
        String title=switch(preset){case 0->"PlayerInventory overview";case 1->"Fluid overview";case 2->"Energy overview";default->throw new IllegalArgumentException("Unknown starter board");};
        var type=preset==0?DisplayElements.Type.INVENTORY:preset==1?DisplayElements.Type.FLUID_GRID:DisplayElements.Type.BAR;
        var header=new DisplayElements.Spec(UUID.randomUUID(),DisplayElements.Type.TEXT,title,"","","",new DisplayElements.Rect(4,4,width-8,14),0x62C7D6,false,false,1,0,0,false,false,DisplayElements.TextAlign.CENTER,false,1F);
        var body=new DisplayElements.Spec(UUID.randomUUID(),type,"","",preset==2?"storage":"","",new DisplayElements.Rect(4,22,width-8,height-26),0x73D86B,true,true,3,0,0,false,false,DisplayElements.TextAlign.LEFT,false,1F);
        return new LayoutTemplate(width,height,List.of(header,body));
    }
    public List<DisplayElements.Spec> prepare(int targetWidth,int targetHeight,boolean fit,boolean clearReaders){
        if(targetWidth<8||targetHeight<9||targetWidth>DisplayElements.MAX_CANVAS||targetHeight>DisplayElements.MAX_CANVAS)throw new IllegalArgumentException("Invalid target canvas");
        List<DisplayElements.Spec> result=new ArrayList<>();
        for(var original:elements){
            var e=original.identity(UUID.randomUUID());var b=e.bounds();
            if(fit){int x=(int)((long)b.x()*targetWidth/width),y=(int)((long)b.y()*targetHeight/height),w=Math.max(8,(int)((long)b.width()*targetWidth/width)),h=Math.max(9,(int)((long)b.height()*targetHeight/height));x=Math.min(x,targetWidth-8);y=Math.min(y,targetHeight-9);b=new DisplayElements.Rect(x,y,Math.min(w,targetWidth-x),Math.min(h,targetHeight-y));}
            if(b.right()>targetWidth||b.bottom()>targetHeight)throw new IllegalArgumentException("Layout exceeds target; enable Fit to screen");
            result.add(new DisplayElements.Spec(e.id(),e.type(),e.text(),clearReaders?"":e.reader(),e.key(),e.asset(),b,e.color(),e.count(),e.names(),e.columns(),e.offset(),e.page(),e.vertical(),e.compact(),e.textAlign(),e.wrap(),e.textScale(),e.options()));
        }
        return List.copyOf(result);
    }
}
