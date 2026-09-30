import com.sun.source.util.JavacTask;
import java.nio.file.*;
import java.util.*;
import javax.tools.*;
/** Parse only. Missing Minecraft dependencies mean this does NOT type-check Minecraft API calls. */
public final class VerifyJavaSyntax {
    public static void main(String[] args)throws Exception{
        Path root=Path.of(args[0]);List<Path> sources;
        try(var paths=Files.walk(root.resolve("src/main/java"))){sources=paths.filter(p->p.toString().endsWith(".java")).toList();}
        var compiler=ToolProvider.getSystemJavaCompiler();var diagnostics=new DiagnosticCollector<JavaFileObject>();
        try(var manager=compiler.getStandardFileManager(diagnostics,null,java.nio.charset.StandardCharsets.UTF_8)){
            var task=(JavacTask)compiler.getTask(null,manager,diagnostics,List.of("-proc:none","--release","21"),null,manager.getJavaFileObjectsFromPaths(sources));
            for(var tree:task.parse()){}
        }
        for(var d:diagnostics.getDiagnostics())if(d.getKind()==Diagnostic.Kind.ERROR)throw new IllegalStateException(d.toString());
        System.out.println("PASS Java 21 AST parsing: "+sources.size()+" production Java files. Syntax only; not Minecraft/NeoForge API type-checking.");
    }
}
