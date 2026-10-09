import java.util.*;
import net.foundations.pl4.core.*;

/** Executes real dependency-free production rules; NOT a substitute for Minecraft GameTests. */
public final class R5RegressionTests {
    private static long assertions;
    private static void check(boolean condition,String message){assertions++;if(!condition)throw new AssertionError(message);}
    private static void close(double a,double b,String message){check(Math.abs(a-b)<1e-6,message+": "+a+" != "+b);}
    private static void rejects(Runnable action){boolean failed=false;try{action.run();}catch(IllegalArgumentException e){failed=true;}check(failed,"Expected invalid argument rejection");}
    public static void main(String[] args){
        connections();graphs();hammer();canvases();
        System.out.println("PASS: "+assertions+" assertions against production ConnectionRules, DisjointSets, HammerMotion, HammerGeometry and DisplayLayout.");
        System.out.println("Scope: dependency-free JVM rules only. No Minecraft API compilation, graphics, server or Windows execution.");
    }
    private static void connections(){
        long start=assertions;
        for(int d=0;d<6;d++)for(int a=0;a<64;a++){
            int toggled=ConnectionRules.toggle(a,d);
            check(ConnectionRules.toggle(toggled,d)==a,"Toggle must round-trip all six faces");
            check((toggled^a)==(1<<d),"Toggle must change exactly one port");
            for(int b=0;b<64;b++)for(int type=0;type<4;type++)for(int occupied=0;occupied<4;occupied++){
                boolean redA=(type&1)!=0,redB=(type&2)!=0;
                int occA=(occupied&1)!=0?1<<d:0,occB=(occupied&2)!=0?1<<(d^1):0;
                boolean expected=redA==redB&&(a&(1<<d))==0&&(b&(1<<(d^1)))==0&&occupied==0;
                boolean actual=ConnectionRules.external(redA,redB,a,b,occA,occB,d);
                check(actual==expected,"External port/colour/obstruction predicate");
                check(actual==ConnectionRules.external(redB,redA,b,a,occB,occA,d^1),"Connection must be bidirectional");
            }
            for(int type=0;type<4;type++)check(ConnectionRules.internal((type&1)!=0,(type&2)!=0,a,d)==(((type&1)!=0)==((type&2)!=0)&&(a&(1<<d))==0),"Internal attachment predicate");
        }
        for(int bad:new int[]{-1,6,100}){rejects(()->ConnectionRules.toggle(0,bad));rejects(()->ConnectionRules.external(false,false,0,0,0,0,bad));}
        close(ConnectionRules.armStart(1),0,"External arm reaches boundary");close(ConnectionRules.armStart(2),1,"Internal arm reaches component");close(ConnectionRules.armStart(3),3,"Short connector reaches reader");rejects(()->ConnectionRules.armStart(0));
        System.out.println("PASS connection rules: "+(assertions-start)+" assertions; all 64x64 port masks, 6 directions, both cable families and obstruction pairs.");
    }
    private static void graphs(){
        long start=assertions;List<int[]> edges=new ArrayList<>();
        for(int y=0;y<3;y++)for(int x=0;x<3;x++){int n=y*3+x;if(x<2)edges.add(new int[]{n,n+1});if(y<2)edges.add(new int[]{n,n+3});}
        for(int mask=0;mask<(1<<edges.size());mask++){
            var actual=new DisjointSets(9);boolean[][] expected=new boolean[9][9];for(int i=0;i<9;i++)expected[i][i]=true;
            for(int e=0;e<edges.size();e++)if((mask&(1<<e))!=0){var edge=edges.get(e);actual.union(edge[0],edge[1]);expected[edge[0]][edge[1]]=expected[edge[1]][edge[0]]=true;}
            for(int k=0;k<9;k++)for(int i=0;i<9;i++)for(int j=0;j<9;j++)expected[i][j]|=expected[i][k]&&expected[k][j];
            for(int i=0;i<9;i++)for(int j=0;j<9;j++)check((actual.find(i)==actual.find(j))==expected[i][j],"Graph connectivity differs from independent transitive closure");
        }
        rejects(()->new DisjointSets(-1));
        System.out.println("PASS graph partitioning: "+(assertions-start)+" assertions across all 4096 edge subsets of a 3x3 network.");
    }
    private static void hammer(){
        long start=assertions;
        check(HammerGeometry.PIECES.size()==16,"Original hammer has 16 pieces");Set<String> names=new HashSet<>();int moving=0,upper=0;
        for(var p:HammerGeometry.PIECES){check(names.add(p.name()),"Unique hammer piece name");check(p.width()>0&&p.height()>0&&p.depth()>0,"Nondegenerate box");check(p.u()+2*p.depth()+2*p.width()<=128&&p.v()+p.depth()+p.height()<=64,"Box UVs inside the original 128x64 skin");if(p.moving())moving++;if(p.upper())upper++;}
        check(moving==1&&upper==6,"Only the weight moves; four beams, top and weight are upper geometry");
        var weight=HammerGeometry.PIECES.stream().filter(HammerGeometry.Piece::moving).findFirst().orElseThrow();
        var pedestal=HammerGeometry.PIECES.stream().filter(p->p.name().equals("pedestal")).findFirst().orElseThrow();
        double impact=1.5-(weight.py()+weight.y()+weight.height()+HammerGeometry.TRAVEL)/16.0;
        double surface=1.5-(pedestal.py()+pedestal.y())/16.0;
        close(impact,surface,"Weight must meet the pedestal after the original 26-pixel stroke");
        for(int duration:new int[]{1,2,11,100,72000})for(int t:new int[]{-1,0,1,2,10,50,100,200,72000,Integer.MAX_VALUE}){
            int pixels=HammerMotion.progressPixels(t,duration);check(pixels>=0&&pixels<=23,"Progress arrow bounded");
            for(double elapsed:new double[]{-2,0,.5,1,10,20,100,Double.NaN,Double.POSITIVE_INFINITY}){
                double progress=HammerMotion.fraction(t,duration,0,200,elapsed,true);check(Double.isFinite(progress)&&progress>=0&&progress<=1,"Stroke finite and bounded");
                double cooldown=HammerMotion.fraction(0,duration,t,200,elapsed,false);check(Double.isFinite(cooldown)&&cooldown>=0&&cooldown<=1,"Cooldown finite and bounded");
            }
        }
        close(HammerMotion.fraction(0,100,200,200,0,false),1,"Return starts at contact");
        close(HammerMotion.fraction(0,100,100,200,0,false),.5,"Half return");
        close(HammerMotion.fraction(0,100,0,200,0,false),0,"Idle at top");
        close(HammerMotion.fraction(25,100,0,200,5,true),.3,"Client interpolation");
        for(int existing=0;existing<=65;existing++)for(int count=0;count<=65;count++)for(boolean same:new boolean[]{false,true}){
            boolean expected=count>0&&(existing==0||same)&&existing+count<=64;
            check(HammerMotion.outputFits(existing,count,64,same)==expected,"Output capacity/component compatibility");
        }
        check(!HammerMotion.outputFits(Integer.MAX_VALUE,1,Integer.MAX_VALUE,true),"Count arithmetic cannot overflow into acceptance");
        check(!HammerMotion.outputFits(-1,1,64,true),"Negative item count rejected");
        System.out.println("PASS hammer geometry/timing/output: "+(assertions-start)+" assertions.");
    }
    private static void canvases(){
        long start=assertions;Set<String> textures=new HashSet<>();for(int m=0;m<16;m++)textures.add(DisplayLayout.texture(m));check(textures.size()==16,"All sixteen original frame states addressed");
        for(int w=1;w<=16;w++)for(int h=1;h<=16;h++){
            List<DisplayLayout.Cell> cells=new ArrayList<>();for(int y=0;y<h;y++)for(int x=0;x<w;x++)cells.add(new DisplayLayout.Cell(x-23,y+7));
            var rect=DisplayLayout.rectangle(cells).orElseThrow();check(rect.width()==w&&rect.height()==h&&rect.x()==-23&&rect.y()==7,"Negative-coordinate rectangle bounds");
            for(var c:cells){int x=c.x()+23,y=c.y()-7,m=rect.mask(c);check(((m&1)!=0)==(x>0)&&((m&2)!=0)==(x<w-1)&&((m&4)!=0)==(y>0)&&((m&8)!=0)==(y<h-1),"Only joined seams are borderless");}
            List<DisplayLayout.Cell> duplicate=new ArrayList<>(cells);duplicate.add(cells.get(0));check(DisplayLayout.rectangle(duplicate).isEmpty(),"Duplicate cell rejected");
            if(w>2&&h>2){cells.remove(new DisplayLayout.Cell(-22,8));check(DisplayLayout.rectangle(cells).isEmpty(),"Holes must not form a canvas");}
        }
        check(DisplayLayout.rectangle(List.of()).isEmpty(),"Empty canvas rejected");
        check(DisplayLayout.rectangle(List.of(new DisplayLayout.Cell(0,0),new DisplayLayout.Cell(1,0),new DisplayLayout.Cell(0,1))).isEmpty(),"L shape remains independent tiles");
        check(DisplayLayout.rectangle(List.of(new DisplayLayout.Cell(Integer.MIN_VALUE,0),new DisplayLayout.Cell(Integer.MAX_VALUE,0))).isEmpty(),"Canvas bounds cannot overflow");
        List<DisplayLayout.Cell> wide=new ArrayList<>();for(int x=0;x<17;x++)wide.add(new DisplayLayout.Cell(x,0));check(DisplayLayout.rectangle(wide).isEmpty(),"Maximum 16 cells per axis");
        System.out.println("PASS rectangular canvases: "+(assertions-start)+" assertions across all 1..16 x 1..16 dimensions.");
    }
}
