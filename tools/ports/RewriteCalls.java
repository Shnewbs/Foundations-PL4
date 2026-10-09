import com.sun.source.tree.*;
import com.sun.source.util.*;
import java.nio.file.*;
import java.util.*;
import javax.tools.*;

/** Explicit API migration using javac source ranges, not receiver-guessing regular expressions. */
public class RewriteCalls {
    record Edit(int from,int to,String text){}
    static final String DATA="net.foundations.pl4.compat.PortData";
    static final String CAPS="net.foundations.pl4.compat.PortCapabilities";
    public static void main(String[] args)throws Exception {
        var compiler=ToolProvider.getSystemJavaCompiler();
        for(Path path:Files.walk(Path.of(args[0])).filter(p->p.toString().endsWith(".java")).toList()) {
            if(path.toString().contains("/compat/"))continue;
            for(int round=0;round<12;round++){
                String source=Files.readString(path);
                var edits=new ArrayList<Edit>();
                try(var manager=compiler.getStandardFileManager(null,null,null)){
                    var task=(JavacTask)compiler.getTask(null,manager,d->{},List.of("-proc:none","--release","21"),null,manager.getJavaFileObjects(path));
                    for(var unit:task.parse()){
                        var positions=Trees.instance(task).getSourcePositions();
                        new TreeScanner<Void,Void>(){
                            String raw(Tree tree){return source.substring((int)positions.getStartPosition(unit,tree),(int)positions.getEndPosition(unit,tree));}
                            @Override public Void visitMethodInvocation(MethodInvocationTree tree,Void unused){
                                super.visitMethodInvocation(tree,unused);
                                if(!(tree.getMethodSelect() instanceof MemberSelectTree member))return null;
                                String method=member.getIdentifier().toString(),receiver=raw(member.getExpression());
                                var a=tree.getArguments().stream().map(this::raw).toList();
                                if(receiver.startsWith("net.foundations.pl4.compat."))return null;
                                String replacement=null;
                                switch(method){
                                    case "getLast" -> {if(a.isEmpty())replacement="net.foundations.pl4.compat.PortLists.last("+receiver+")";}
                                    case "getComponentsPatch" -> replacement=DATA+".components("+receiver+")";
                                    case "copyWithAmount" -> replacement=DATA+".copyWithAmount("+receiver+","+String.join(",",a)+")";
                                    case "get","set","remove","has" -> {if(!a.isEmpty()&&a.get(0).contains("DataComponents."))replacement=DATA+"."+method+"("+receiver+","+String.join(",",a)+")";}
                                    case "getCapability" -> {if(a.size()==3&&!receiver.equals("super"))replacement=CAPS+".get("+receiver+","+String.join(",",a)+")";}
                                    case "save" -> {if(a.size()==1&&Set.of("item","fluid","pendingItem","pendingFluid","stack","output").contains(receiver.substring(receiver.lastIndexOf('.')+1)))replacement=DATA+".save("+receiver+","+a.get(0)+")";}
                                    case "parseOptional" -> {if(receiver.equals("ItemStack")||receiver.equals("FluidStack"))replacement=DATA+".parse"+(receiver.equals("ItemStack")?"Item":"Fluid")+"("+String.join(",",a)+")";}
                                }
                                if(replacement!=null){int from=(int)positions.getStartPosition(unit,tree),to=(int)positions.getEndPosition(unit,tree);
                                    if(edits.stream().noneMatch(e->e.from>=from&&e.to<=to))edits.add(new Edit(from,to,replacement));
                                }
                                return null;
                            }
                        }.scan(unit,null);
                    }
                }
                if(edits.isEmpty())break;
                edits.sort(Comparator.comparingInt(Edit::from).reversed());
                StringBuilder out=new StringBuilder(source);
                for(var edit:edits)out.replace(edit.from,edit.to,edit.text);
                Files.writeString(path,out);
            }
        }
    }
}
