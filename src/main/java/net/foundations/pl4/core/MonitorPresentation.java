package net.foundations.pl4.core;

/** Presentation-only geometry. R11 large joined boards use a dynamic whole-surface logical canvas; ordinary panels remain 248x120. */
public final class MonitorPresentation {
    public record Region(int x,int y,int width,int height) {
        public int right(){return x+width;} public int bottom(){return y+height;}
    }
    public record Table(Region area,int inset,int titleY,int ruleY,int firstRowY,int rowHeight,int rows,int footerY) {
        public int left(){return area.x()+inset;}
        public int right(){return area.right()-inset;}
        public int contentWidth(){return area.width()-inset*2;}
        public int rowY(int row){return firstRowY+row*rowHeight;}
    }
    public static Region area(boolean large,int columns,int rows,double scale) {
        if(!Double.isFinite(scale)||scale<=0)throw new IllegalArgumentException("Invalid canvas scale");
        if(!large)return new Region(0,0,248,120);
        var space=DynamicCanvasLayout.large(columns,rows);return new Region(0,0,space.width(),space.height());
    }
    public static Table table(Region area,int available) {
        int inset=6,top=area.y()+6,rowHeight=12,first=top+28,footer=area.bottom()-15;
        int count=Math.clamp(Math.min(available,Math.max(0,(footer-first-5)/rowHeight)),0,24);
        return new Table(area,inset,top,top+14,first,rowHeight,count,footer);
    }
    public static String title(String label,String status) {
        if(label!=null&&!label.isBlank())return label;
        if(status==null||status.isBlank())return "Monitor";
        String name=status.replaceFirst(" \\(\\d+ x \\d+\\)$", "");
        return switch(name){
            case "inventoryreader"->"Inventory";case "fluidreader"->"Fluids";
            case "energyreader"->"Energy";case "inforeader"->"Information";
            case "networkreader"->"Network";default->name;
        };
    }
    public static String amount(DisplayElements.Sample row) {
        String value=DisplayElements.number(row.value(),true);
        if(row.capacity()>0)value+=" / "+DisplayElements.number(row.capacity(),true);
        String unit=row.unit();
        // The inventory's purpose is in the heading; avoid "1 items" after every name.
        return unit==null||unit.isBlank()||unit.equals("items")?value:value+" "+unit;
    }
    public static String footer(int shown,int total) {
        return total==0?"No rows  /  AUTO LIST":shown+" / "+total+" rows  /  AUTO LIST";
    }
    public static boolean automatic(DisplayElements.Mode mode,boolean editing) {
        return !editing&&mode==DisplayElements.Mode.AUTO_LIST;
    }
    private MonitorPresentation(){}
}
