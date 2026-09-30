package net.foundations.pl4.core;

import java.util.*;

/** Minecraft-independent display contract, used by persistence, packets, editor and rendering.
 * Coordinates are logical screen pixels, NOT blocks. Reader mode is deliberately not a presentation type.
 */
public final class DisplayElements {
    public static final int WIDTH=248, HEIGHT=120, MAX_CANVAS=4096, MAX_ELEMENTS=32, MAX_PAGES=8, MAX_ICONS=128;
    public enum Type { TEXT, ITEM, BLOCK, INVENTORY, FLUID, FLUID_GRID, BAR;
        public static Type parse(String name){try{return valueOf(name.toUpperCase(Locale.ROOT));}catch(Exception e){return TEXT;}}
    }
    public enum Mode { AUTO_LIST, CUSTOM;
        public static Mode parse(String name){return "CUSTOM".equals(name)?CUSTOM:AUTO_LIST;}
    }
    public enum TextAlign { LEFT, CENTER, RIGHT;
        public static TextAlign parse(String name){try{return valueOf(name.toUpperCase(Locale.ROOT));}catch(Exception e){return LEFT;}}
    }
    public record Rect(int x,int y,int width,int height){
        public int right(){return x+width;}public int bottom(){return y+height;}
        public boolean contains(double a,double b){return a>=x&&a<right()&&b>=y&&b<bottom();}
    }
    public static final float MIN_TEXT_SCALE=0.25F,MAX_TEXT_SCALE=4F;
    public record Spec(UUID id,Type type,String text,String reader,String key,String asset,Rect bounds,
                       int color,boolean count,boolean names,int columns,int offset,int page,boolean vertical,boolean compact,
                       TextAlign textAlign,boolean wrap,float textScale) {
        public Spec {
            id=Objects.requireNonNull(id);type=Objects.requireNonNull(type);
            textAlign=Objects.requireNonNullElse(textAlign,TextAlign.LEFT);
            text=clean(text,128);reader=clean(reader,64);key=clean(key,192);asset=clean(asset,192);
            int x=Math.clamp(bounds.x(),0,MAX_CANVAS-8),y=Math.clamp(bounds.y(),0,MAX_CANVAS-9);
            bounds=new Rect(x,y,Math.clamp(bounds.width(),8,MAX_CANVAS-x),Math.clamp(bounds.height(),9,MAX_CANVAS-y));
            color&=0xFFFFFF;columns=Math.clamp(columns,1,16);offset=Math.clamp(offset,0,65535);page=Math.clamp(page,0,MAX_PAGES-1);
            textScale=Double.isFinite(textScale)&&textScale>0?Math.clamp(textScale,MIN_TEXT_SCALE,MAX_TEXT_SCALE):1F;
        }
        public Spec(UUID id,Type type,String text,String reader,String key,String asset,Rect bounds,
                    int color,boolean count,boolean names,int columns,int offset,int page,boolean vertical,boolean compact){
            this(id,type,text,reader,key,asset,bounds,color,count,names,columns,offset,page,vertical,compact,TextAlign.LEFT,false,1F);
        }
        public Spec bounds(Rect r){return new Spec(id,type,text,reader,key,asset,r,color,count,names,columns,offset,page,vertical,compact,textAlign,wrap,textScale);}
        public Spec identity(UUID value){return new Spec(value,type,text,reader,key,asset,bounds,color,count,names,columns,offset,page,vertical,compact,textAlign,wrap,textScale);}
        public Spec onPage(int value){return new Spec(id,type,text,reader,key,asset,bounds,color,count,names,columns,offset,value,vertical,compact,textAlign,wrap,textScale);}
        public Spec textStyle(TextAlign alignment,boolean wrapped){return new Spec(id,type,text,reader,key,asset,bounds,color,count,names,columns,offset,page,vertical,compact,alignment,wrapped,textScale);}
        public Spec textStyle(TextAlign alignment,boolean wrapped,float scale){return new Spec(id,type,text,reader,key,asset,bounds,color,count,names,columns,offset,page,vertical,compact,alignment,wrapped,scale);}
    }
    public interface Sample {
        String key();String name();double value();double capacity();String unit();
        boolean hasItem();boolean hasBlock();boolean hasFluid();
        default String itemId(){return "";}default String fluidId(){return "";}
    }
    public sealed interface Draw permits Box,Text,Icon,Liquid {}
    public record Box(Rect rect,int color,boolean filled,int layer) implements Draw {
        public Box { layer=Math.clamp(layer,1,3); }
    }
    public record Text(String value,int x,int y,int width,int height,int color,TextAlign alignment,boolean wrap,float scale,boolean overlay) implements Draw {
        public Text {
            scale=Double.isFinite(scale)&&scale>0?Math.clamp(scale,MIN_TEXT_SCALE,MAX_TEXT_SCALE):1F;
        }
        public Text(String value,int x,int y,int width,int color,boolean right,boolean overlay){
            this(value,x,y,width,12,color,right?TextAlign.RIGHT:TextAlign.LEFT,false,1F,overlay);
        }
    }
    public record Icon(int sample,Rect rect,boolean block) implements Draw {}
    public record Liquid(int sample,Rect rect,double fraction) implements Draw {}
    public record Scene(List<Draw> draws,String diagnostic){public Scene{draws=List.copyOf(draws);}}
    public static Spec create(Type type,int page){return create(type,page,WIDTH,HEIGHT);}
    public static Spec create(Type type,int page,int spaceW,int spaceH){
        int w=switch(type){case TEXT,BAR->Math.min(120,Math.max(8,spaceW-8));case INVENTORY,FLUID_GRID->Math.min(232,Math.max(8,spaceW-8));default->Math.min(32,Math.max(8,spaceW-8));};
        int h=switch(type){case TEXT->14;case BAR->20;case INVENTORY,FLUID_GRID->Math.min(96,Math.max(9,spaceH-8));default->Math.min(40,Math.max(9,spaceH-8));};
        return new Spec(UUID.randomUUID(),type,"","","","",new Rect(4,4,w,h),0xFFFFFF,true,false,defaultColumns(type),0,page,false,true);
    }
    public static int defaultColumns(Type type){return type==Type.INVENTORY||type==Type.FLUID_GRID?3:1;}
    public static Spec move(Spec s,double dx,double dy,boolean resize,boolean snap){return move(s,dx,dy,resize,snap,WIDTH,HEIGHT);}
    public static Spec move(Spec s,double dx,double dy,boolean resize,boolean snap,int spaceW,int spaceH){
        spaceW=Math.clamp(spaceW,8,MAX_CANVAS);spaceH=Math.clamp(spaceH,9,MAX_CANVAS);
        int x=s.bounds.x(),y=s.bounds.y(),w=s.bounds.width(),h=s.bounds.height();int step=snap?4:1;
        x=Math.clamp(x,0,Math.max(0,spaceW-8));y=Math.clamp(y,0,Math.max(0,spaceH-9));w=Math.clamp(w,8,Math.max(8,spaceW-x));h=Math.clamp(h,9,Math.max(9,spaceH-y));
        if(resize){w=(int)Math.round((w+dx)/step)*step;h=(int)Math.round((h+dy)/step)*step;w=Math.clamp(w,8,Math.max(8,spaceW-x));h=Math.clamp(h,9,Math.max(9,spaceH-y));}
        else{x=(int)Math.round((x+dx)/step)*step;y=(int)Math.round((y+dy)/step)*step;x=Math.clamp(x,0,Math.max(0,spaceW-w));y=Math.clamp(y,0,Math.max(0,spaceH-h));}
        return s.bounds(new Rect(x,y,w,h));
    }
    public static String clean(String s,int max){if(s==null)return "";String out=s.replaceAll("[\\p{Cntrl}§]","");return out.substring(0,Math.min(max,out.length()));}
    public static double fraction(double value,double capacity){return Double.isFinite(value)&&Double.isFinite(capacity)&&capacity>0?Math.clamp(value/capacity,0,1):0;}
    public static String number(double value,boolean compact){
        if(!Double.isFinite(value))return "?";
        if(compact&&Math.abs(value)>=1000){String[] units={"k","M","G","T","P","E"};int n=-1;double v=value;do{v/=1000;n++;}while(Math.abs(v)>=1000&&n<units.length-1);return String.format(Locale.ROOT,Math.abs(v)<10?"%.1f%s":"%.0f%s",v,units[n]);}
        return value==(long)value?Long.toString((long)value):String.format(Locale.ROOT,"%.2f",value);
    }
    private static int select(Spec e,List<? extends Sample> rows){
        for(int i=0;i<rows.size();i++){Sample r=rows.get(i);if(!e.key.isEmpty()&&!r.key().equals(e.key)&&!r.itemId().equals(e.key)&&!r.fluidId().equals(e.key))continue;
            boolean valid=switch(e.type){case ITEM,INVENTORY->r.hasItem();case BLOCK->r.hasBlock();case FLUID,FLUID_GRID->r.hasFluid();default->true;};
            if(valid)return i;
        }return -1;
    }
    public static Scene plan(Spec e,List<? extends Sample> rows){
        List<Draw> out=new ArrayList<>();Rect b=e.bounds;int selected=select(e,rows);String diagnostic="";
        switch(e.type){
            case TEXT -> {
                String value=e.text;
                if(!e.key.isBlank())value+=(value.isEmpty()?"":" ")+(selected<0?"[missing data]":number(rows.get(selected).value(),e.compact)+(rows.get(selected).unit().isBlank()?"":" "+rows.get(selected).unit()));
                out.add(new Text(value,b.x,b.y,b.width,b.height,e.color,e.textAlign,e.wrap,e.textScale,false));
            }
            case BAR -> {
                out.add(new Box(b,0xFF333333,true,1));out.add(new Box(b,0xFFB0B0B0,false,3));
                if(selected>=0){Sample s=rows.get(selected);double fill=fraction(s.value(),s.capacity());
                    Rect inner=new Rect(b.x+1,b.y+1,b.width-2,b.height-2);
                    if(e.vertical){int h=(int)Math.round(inner.height*fill);if(h>0)out.add(new Box(new Rect(inner.x,inner.bottom()-h,inner.width,h),0xFF000000|e.color,true,2));}
                    else{int w=(int)Math.round(inner.width*fill);if(w>0)out.add(new Box(new Rect(inner.x,inner.y,w,inner.height),0xFF000000|e.color,true,2));}
                    String label=s.capacity()>0?number(s.value(),e.compact)+" / "+number(s.capacity(),e.compact)+" "+s.unit():"No capacity";
                    if(e.count&&b.height>=11)out.add(new Text(label,b.x+2,b.y+(b.height-9)/2,b.width-4,0xFFFFFF,false,true));
                }else diagnostic="Select numeric data";
            }
            case ITEM,BLOCK,FLUID -> {
                if(selected<0){diagnostic=switch(e.type){case BLOCK->"Select a block item";case FLUID->"Select a fluid";default->"Select an item";};break;}
                Sample s=rows.get(selected);boolean names=e.names&&b.height>=20;int space=names?11:0;int size=Math.min(b.width,b.height-space);
                Rect box=new Rect(b.x,b.y,Math.max(1,size),Math.max(1,size));
                if(e.type==Type.FLUID){out.add(new Box(box,0xFF343434,true,1));out.add(new Liquid(selected,new Rect(box.x+1,box.y+1,Math.max(1,box.width-2),Math.max(1,box.height-2)),s.capacity()>0?fraction(s.value(),s.capacity()):1));out.add(new Box(box,0xFFB0B0B0,false,3));}
                else out.add(new Icon(selected,box,e.type==Type.BLOCK));
                if(e.count&&box.height>=9)out.add(new Text(number(s.value(),e.compact),box.x+1,box.bottom()-9,box.width-2,e.color,true,true));
                if(names)out.add(new Text(e.text.isEmpty()?s.name():e.text,b.x,b.y+size+1,b.width,e.color,false,false));
            }
            case INVENTORY,FLUID_GRID -> {
                int cols=Math.min(e.columns,Math.max(1,b.width/18));int cell=b.width/cols;
                int icon=Math.min(32,cell-2);int stepY=icon+2+(e.names?10:0);
                int limit=Math.min(MAX_ICONS,cols*Math.max(0,b.height/stepY)),seen=0,drawn=0;
                for(int i=0;i<rows.size()&&drawn<limit;i++){
                    Sample s=rows.get(i);if(e.type==Type.INVENTORY?!s.hasItem():!s.hasFluid())continue;
                    if(!e.key.isBlank()&&!s.key().equals(e.key)&&!s.itemId().equals(e.key)&&!s.fluidId().equals(e.key))continue;
                    if(seen++<e.offset)continue;
                    int x=b.x+(drawn%cols)*cell,y=b.y+(drawn/cols)*stepY;Rect box=new Rect(x,y,icon,icon);
                    if(e.type==Type.INVENTORY)out.add(new Icon(i,box,false));
                    else{out.add(new Box(box,0xFF343434,true,1));out.add(new Liquid(i,new Rect(x+1,y+1,icon-2,icon-2),1));out.add(new Box(box,0xFFB0B0B0,false,3));}
                    if(e.count&&icon>=9)out.add(new Text(number(s.value(),e.compact),x+1,y+icon-9,icon-2,e.color,true,true));
                    if(e.names)out.add(new Text(s.name(),x,y+icon+1,cell-2,e.color,false,false));drawn++;
                }
                if(drawn==0)diagnostic=limit==0?"Enlarge grid":"No matching "+(e.type==Type.INVENTORY?"items":"fluids");
            }
        }
        if(!diagnostic.isEmpty()){out.add(new Box(b,0xFFA56262,false,3));out.add(new Text(diagnostic,b.x+2,b.y+Math.min(2,b.height-9),Math.max(1,b.width-4),0xF2A7A7,false,true));}
        return new Scene(out,diagnostic);
    }
    /** Model-space hundredths; divide by 16, then by canvas scale before applying to the pose.
     * A block-depth value of .03 is intentionally never used. Item geometry fits inside its layer. */
    public static double worldDepth(int layer){return Math.clamp(layer,0,8)*.01/16.0;}
    public static double logicalDepth(int layer,double scale){if(!Double.isFinite(scale)||scale<=0)throw new IllegalArgumentException("Invalid canvas scale");return worldDepth(layer)/scale;}
    private DisplayElements(){}
}
