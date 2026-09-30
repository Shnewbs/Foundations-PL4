import java.util.*;
import net.foundations.pl4.core.*;

public final class R9RegressionTests {
    private static int assertions;
    private static void check(boolean b,String why){assertions++;if(!b)throw new AssertionError(why);}
    private static void near(double a,double b,String why){check(Math.abs(a-b)<1e-7,why+": "+a+" vs "+b);}
    record Sample(String key,String name,double value,double capacity,String unit,boolean hasItem,boolean hasBlock,boolean hasFluid) implements DisplayElements.Sample {
        public String itemId(){return hasItem?key.split("~")[0]:"";}public String fluidId(){return hasFluid?key:"";}
    }
    static final List<Sample> DATA=List.of(new Sample("minecraft:stone","Stone",17,0,"items",true,true,false),new Sample("minecraft:diamond~named","Named diamond",10000,0,"items",true,false,false),new Sample("minecraft:water","Water",500,1000,"mB",false,false,true),new Sample("storage","Energy",2500,10000,"FE",false,false,false));
    static DisplayElements.Spec spec(DisplayElements.Type t,int x,int y,int w,int h){var s=DisplayElements.create(t,0);return s.bounds(new DisplayElements.Rect(x,y,w,h));}
    static void types(){
        for(var type:DisplayElements.Type.values()){
            var s=spec(type,0,0,220,100);var plan=DisplayElements.plan(s,DATA);check(plan.diagnostic().isEmpty(),"valid type "+type);
            if(type==DisplayElements.Type.ITEM||type==DisplayElements.Type.BLOCK||type==DisplayElements.Type.INVENTORY)check(plan.draws().stream().anyMatch(x->x instanceof DisplayElements.Icon),"must create item/model commands");
            if(type==DisplayElements.Type.BLOCK)check(plan.draws().stream().filter(x->x instanceof DisplayElements.Icon).map(x->(DisplayElements.Icon)x).allMatch(DisplayElements.Icon::block),"block renderer flag");
            if(type==DisplayElements.Type.FLUID||type==DisplayElements.Type.FLUID_GRID)check(plan.draws().stream().anyMatch(x->x instanceof DisplayElements.Liquid),"fluid commands");
            if(type==DisplayElements.Type.BAR)check(plan.draws().stream().anyMatch(x->x instanceof DisplayElements.Box),"bar rectangles not pipe characters");
        }
        var block=DisplayElements.plan(spec(DisplayElements.Type.BLOCK,0,0,40,40),List.of(DATA.get(1)));
        check(!block.diagnostic().isEmpty()&&block.draws().stream().noneMatch(x->x instanceof DisplayElements.Icon),"item cannot silently pretend to be block");
        var grid=DisplayElements.plan(spec(DisplayElements.Type.INVENTORY,0,0,220,100),DATA);
        check(grid.draws().stream().filter(x->x instanceof DisplayElements.Icon).count()==2,"inventory excludes fluids/energy");
        var missing=DisplayElements.plan(spec(DisplayElements.Type.ITEM,0,0,40,40),List.of(DATA.get(3)));check(!missing.diagnostic().isEmpty(),"no inventory-list fallback for numeric data");
        near(DisplayElements.fraction(500,1000),.5,"fraction");near(DisplayElements.fraction(Double.NaN,100),0,"nan");near(DisplayElements.fraction(1,0),0,"zero capacity");near(DisplayElements.fraction(-5,100),0,"negative");near(DisplayElements.fraction(500,100),1,"overflow");
        check(DisplayElements.number(17,false).equals("17"),"exact count");check(DisplayElements.number(12000,true).equals("12k"),"compact count");
        var item=DisplayElements.create(DisplayElements.Type.ITEM,0);var exact=new DisplayElements.Spec(item.id(),item.type(),"","","minecraft:diamond~named","",item.bounds(),item.color(),true,false,8,0,0,false,false);
        var icon=(DisplayElements.Icon)DisplayElements.plan(exact,DATA).draws().stream().filter(x->x instanceof DisplayElements.Icon).findFirst().orElseThrow();check(icon.sample()==1,"exact variant selected");
    }
    static void bounds(){
        Random random=new Random(8189);for(int n=0;n<4000;n++){
            var type=DisplayElements.Type.values()[n%7];var e=spec(type,random.nextInt(600)-300,random.nextInt(280)-100,random.nextInt(500)-20,random.nextInt(260)-20);
            var moved=DisplayElements.move(e,random.nextInt(700)-350,random.nextInt(400)-200,n%2==0,n%3==0);var b=moved.bounds();
            check(b.x()>=0&&b.y()>=0&&b.right()<=248&&b.bottom()<=120,"clamped geometry");
            var plan=DisplayElements.plan(moved,DATA);for(var op:plan.draws()){
                DisplayElements.Rect rect=null;
                if(op instanceof DisplayElements.Box box)rect=box.rect();if(op instanceof DisplayElements.Icon icon)rect=icon.rect();if(op instanceof DisplayElements.Liquid liquid)rect=liquid.rect();
                if(rect!=null)check(rect.x()>=b.x()&&rect.y()>=b.y()&&rect.right()<=b.right()&&rect.bottom()<=b.bottom()&&rect.width()>0&&rect.height()>0,"draw inside element "+type);
                if(op instanceof DisplayElements.Text text)check(text.x()>=b.x()&&text.y()>=b.y()&&text.x()+text.width()<=b.right()&&text.y()+9<=b.bottom(),"whole text line inside bounds "+type+" "+text+" "+b);
            }
        }
        for(double scale:new double[]{.0018,.0035,.01,.06,1})for(int layer=0;layer<4;layer++)near(DisplayElements.logicalDepth(layer,scale)*scale,DisplayElements.worldDepth(layer),"scale-independent depth");
        check(DisplayElements.worldDepth(3)<.002&&DisplayElements.worldDepth(2)>DisplayElements.worldDepth(1)&&DisplayElements.worldDepth(3)>DisplayElements.worldDepth(2),"icon < quantity < editor, under .002 block");
    }
    static void transactions(){
        var state=new LayoutTransactions.State(List.of(),DisplayElements.Mode.AUTO_LIST,0,0);UUID zero=new UUID(0,0);
        var e=DisplayElements.create(DisplayElements.Type.BLOCK,0);var add=LayoutTransactions.apply(state,0,"add",e.id(),e,"");check(add.accepted()&&add.state().mode()==DisplayElements.Mode.CUSTOM,"adding enters custom mode");
        var stale=LayoutTransactions.apply(add.state(),0,"delete",e.id(),null,"");check(!stale.accepted()&&stale.state().equals(add.state()),"stale edit rejected atomically");
        var duplicate=LayoutTransactions.apply(add.state(),1,"add",e.id(),e,"");check(!duplicate.accepted(),"duplicate UUID rejected");
        var clear=LayoutTransactions.apply(add.state(),1,"clear",zero,null,"");check(clear.accepted()&&clear.state().elements().isEmpty()&&clear.state().mode()==DisplayElements.Mode.CUSTOM,"empty custom screen stays empty");
        for(int i=-5;i<13;i++)check(LayoutTransactions.apply(clear.state(),2,"page",zero,null,""+i).accepted()==(i>=0&&i<8),"page bounds");
        var s=state;for(int n=0;n<32;n++){e=DisplayElements.create(DisplayElements.Type.ITEM,n%8);var result=LayoutTransactions.apply(s,s.revision(),"add",e.id(),e,"");check(result.accepted(),"32 elements permitted");s=result.state();}
        e=DisplayElements.create(DisplayElements.Type.TEXT,0);check(!LayoutTransactions.apply(s,s.revision(),"add",e.id(),e,"").accepted(),"33rd rejected");
        check(!LayoutTransactions.apply(s,s.revision(),"execute",zero,null,"arbitrary").accepted(),"unknown operation rejected");
        check(!LayoutTransactions.apply(new LayoutTransactions.State(List.of(),DisplayElements.Mode.CUSTOM,0,Long.MAX_VALUE),Long.MAX_VALUE,"clear",zero,null,"").accepted(),"revision overflow rejected");
        var scoped=state;for(int n=0;n<9;n++){var old=DisplayElements.create(DisplayElements.Type.ITEM,0);e=new DisplayElements.Spec(old.id(),old.type(),"","source"+n,"","",old.bounds(),old.color(),true,false,8,0,0,false,false);var result=LayoutTransactions.apply(scoped,scoped.revision(),"add",e.id(),e,"");check(result.accepted()==(n<8),"source budget");if(result.accepted())scoped=result.state();}
    }
    static double[] multiply(double[] a,double[] b){double[] o=new double[16];for(int r=0;r<4;r++)for(int c=0;c<4;c++)for(int k=0;k<4;k++)o[c*4+r]+=a[k*4+r]*b[c*4+k];return o;}
    static void picking(){
        double[] identity={1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1};check(DisplayPicking.inverse(new double[16]).isEmpty(),"singular matrix");
        double[] bad=identity.clone();bad[0]=Double.NaN;check(DisplayPicking.inverse(bad).isEmpty(),"nan matrix");
        var inverse=DisplayPicking.inverse(identity).orElseThrow();var p=DisplayPicking.hit(inverse,250,100,500,200).orElseThrow();near(p.x(),0,"identity x");near(p.y(),0,"identity y");
        Random rand=new Random(9);
        for(int n=0;n<1000;n++){
            double sx=.002+rand.nextDouble()*.05,sy=.002+rand.nextDouble()*.05,angle=rand.nextDouble()*6.2;
            double c=Math.cos(angle),s=Math.sin(angle);double[] m={c*sx,s*sx,0,0,-s*sy,c*sy,0,0,0,0,.1,0,-.5,-.3,0,1};
            inverse=DisplayPicking.inverse(m).orElseThrow();double[] product=multiply(m,inverse);for(int i=0;i<16;i++)near(product[i],identity[i],"inverse product");
            double x=rand.nextDouble()*248,y=rand.nextDouble()*120,nx=m[0]*x+m[4]*y+m[12],ny=m[1]*x+m[5]*y+m[13];
            p=DisplayPicking.hit(inverse,(nx+1)*500,(1-ny)*300,1000,600).orElseThrow();near(p.x(),x,"unproject x");near(p.y(),y,"unproject y");
        }
    }
    static void perspectivePicking(){
        double nearPlane=.1,farPlane=256,f=1/Math.tan(Math.toRadians(67)/2);
        double[] projection={f/(5.0/3),0,0,0,0,f,0,0,0,0,(farPlane+nearPlane)/(nearPlane-farPlane),-1,0,0,2*farPlane*nearPlane/(nearPlane-farPlane),0};
        Random random=new Random(913);
        for(int n=0;n<500;n++){
            double ax=random.nextDouble()*Math.PI*2,ay=random.nextDouble()*Math.PI*2,cx=Math.cos(ax),sx=Math.sin(ax),cy=Math.cos(ay),sy=Math.sin(ay);
            double[] rx={1,0,0,0,0,cx,sx,0,0,-sx,cx,0,0,0,0,1},ry={cy,0,-sy,0,0,1,0,0,sy,0,cy,0,0,0,0,1};
            double scale=.002+random.nextDouble()*.06,dist=16+random.nextDouble()*30;
            double[] pose={scale,0,0,0,0,-scale,0,0,0,0,scale,0,-124*scale,60*scale,0,1},translation={1,0,0,0,0,1,0,0,0,0,1,0,0,0,-dist,1};
            double[] matrix=multiply(projection,multiply(translation,multiply(ry,multiply(rx,pose))));
            var inverse=DisplayPicking.inverse(matrix).orElseThrow();double x=random.nextDouble()*265-17,y=random.nextDouble()*120;
            double w=matrix[3]*x+matrix[7]*y+matrix[15],nx=(matrix[0]*x+matrix[4]*y+matrix[12])/w,ny=(matrix[1]*x+matrix[5]*y+matrix[13])/w;
            var point=DisplayPicking.hit(inverse,(nx+1)*500,(1-ny)*300,1000,600).orElseThrow();
            check(Math.abs(point.x()-x)<1e-5&&Math.abs(point.y()-y)<1e-5,"Perspective and arbitrary mount preserve cursor coordinates");
        }
    }
    static void arrangement(){
        var a=DisplayElements.create(DisplayElements.Type.TEXT,0).bounds(new DisplayElements.Rect(20,15,20,12)).textStyle(DisplayElements.TextAlign.RIGHT,true,1.5F);
        var b=DisplayElements.create(DisplayElements.Type.BAR,0).bounds(new DisplayElements.Rect(60,40,30,18));
        var c=DisplayElements.create(DisplayElements.Type.ITEM,0).bounds(new DisplayElements.Rect(120,70,10,20));
        var hidden=DisplayElements.create(DisplayElements.Type.TEXT,1);
        var before=new LayoutTransactions.State(List.of(a,b,c,hidden),DisplayElements.Mode.CUSTOM,0,7);
        for(String action:List.of("align_left","align_right","align_top","align_bottom","align_hcenter","align_vcenter")){
            var r=LayoutTransactions.applyArrange(before,7,action,List.of(a.id()),248,120);
            check(r.accepted()&&r.state().revision()==8,"single canvas alignment "+action);
            var changed=r.state().elements().getFirst();var box=changed.bounds();
            int expectedX=switch(action){case "align_left"->0;case "align_right"->228;case "align_hcenter"->114;default->20;};
            int expectedY=switch(action){case "align_top"->0;case "align_bottom"->108;case "align_vcenter"->54;default->15;};
            check(box.x()==expectedX&&box.y()==expectedY,"canvas anchor "+action);
            check(changed.textAlign()==a.textAlign()&&changed.wrap()&&changed.textScale()==a.textScale(),"arrangement preserves text style");
            check(r.state().elements().subList(1,4).equals(before.elements().subList(1,4)),"unselected and other-page elements preserved");
        }
        var aligned=LayoutTransactions.applyArrange(before,7,"align_bottom",List.of(a.id(),b.id()),248,120);
        check(aligned.accepted()&&aligned.state().elements().getFirst().bounds().bottom()==58,"selection anchor, not canvas anchor");
        var spaced=LayoutTransactions.applyArrange(before,7,"distribute_x",List.of(c.id(),a.id(),b.id()),248,120);
        check(spaced.accepted(),"unordered selection can distribute");
        var list=spaced.state().elements();check(list.get(0).equals(a)&&list.get(2).equals(c),"distribution fixes outer elements");
        check(list.get(1).bounds().x()==65&&list.get(1).bounds().y()==40,"equal 25-pixel edge gaps, unchanged other axis");
        check(!LayoutTransactions.applyArrange(before,6,"align_left",List.of(a.id()),248,120).accepted(),"stale arrangement rejected");
        check(!LayoutTransactions.applyArrange(before,7,"align_left",List.of(hidden.id()),248,120).accepted(),"other page rejected");
        check(!LayoutTransactions.applyArrange(before,7,"align_left",List.of(a.id(),UUID.randomUUID()),248,120).accepted(),"missing identity rejects entire operation");
        check(!LayoutTransactions.applyArrange(before,7,"align_left",List.of(a.id(),a.id()),248,120).accepted(),"duplicate selection rejected");
        check(!LayoutTransactions.applyArrange(before,7,"distribute_x",List.of(a.id(),b.id()),248,120).accepted(),"three required for spacing");
        check(!LayoutTransactions.applyArrange(before,7,"execute",List.of(a.id()),248,120).accepted(),"unknown arrangement rejected");
        var overlap=new LayoutTransactions.State(List.of(a,b.bounds(new DisplayElements.Rect(25,20,30,18)),c.bounds(new DisplayElements.Rect(30,30,10,20))),DisplayElements.Mode.CUSTOM,0,7);
        check(!LayoutTransactions.applyArrange(overlap,7,"distribute_x",List.of(a.id(),b.id(),c.id()),248,120).accepted(),"insufficient spacing rejects atomically");
        check(LayoutTransactions.applyReplace(spaced.state(),8,before.elements()).state().elements().equals(before.elements()),"undo restores arranged layout and styles");
        check(!LayoutTransactions.applyArrange(new LayoutTransactions.State(before.elements(),before.mode(),0,Long.MAX_VALUE),Long.MAX_VALUE,"align_left",List.of(a.id()),248,120).accepted(),"arrangement revision overflow rejected");
    }
    public static void main(String[] args){types();bounds();transactions();picking();perspectivePicking();arrangement();System.out.println("PASS R9 production display planner / transactions / cursor projection: "+assertions+" assertions. No Minecraft rendering or native API compilation.");}
}
