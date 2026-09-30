import com.sun.source.tree.*;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import javax.tools.*;

/**
 * Source-structure regression guard for the PL4 1.21.1 background-order bug.
 * Parses with the JDK, without resolving Minecraft classes or starting a client.
 * This checks call order, not visual correctness or full API compilation.
 */
public final class VerifyScreenLayers {
    private static final String CLIENT = "src/main/java/net/foundations/pl4/client/";

    public static void main(String[] args) throws IOException {
        if (args.length != 1) throw new IllegalArgumentException("Usage: VerifyScreenLayers <source-project>");
        Path root = Path.of(args[0]);
        verify(root.resolve(CLIENT + "PartScreen.java"), true);
        verify(root.resolve(CLIENT + "GuideScreen.java"), false);
        System.out.println("PASS: both PL4 screens finish the native background before content; row tooltips follow widgets.");
    }

    private static void verify(Path file, boolean partScreen) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("Run with a Java 21 JDK, not a JRE.");
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        Map<String, MethodTree> methods = new HashMap<>();
        try (StandardJavaFileManager manager = compiler.getStandardFileManager(diagnostics, null, java.nio.charset.StandardCharsets.UTF_8)) {
            JavacTask task = (JavacTask) compiler.getTask(null, manager, diagnostics,
                    List.of("-proc:none"), null, manager.getJavaFileObjects(file));
            for (CompilationUnitTree unit : task.parse()) {
                new TreeScanner<Void, Void>() {
                    @Override public Void visitClass(ClassTree tree, Void unused) {
                        if (tree.getSimpleName().toString().equals(file.getFileName().toString().replace(".java", ""))) {
                            for (Tree member : tree.getMembers()) {
                                if (member instanceof MethodTree method) {
                                    String name = method.getName().toString();
                                    if ((name.equals("render") || name.equals("renderBackground"))
                                            && methods.put(name, method) != null) fail(file, "Ambiguous rendering overload: " + name);
                                }
                            }
                        }
                        return null;
                    }
                }.scan(unit, null);
            }
        }
        for (Diagnostic<?> diagnostic : diagnostics.getDiagnostics()) {
            if (diagnostic.getKind() == Diagnostic.Kind.ERROR) fail(file, "Java syntax: " + diagnostic);
        }
        MethodTree background = methods.get("renderBackground");
        if (background == null) fail(file, "Missing renderBackground hook; content must not precede Screen.render's native background.");
        requireFirstCall(file, background, "super.renderBackground");
        List<String> backgroundCalls = calls(background);
        if (Collections.frequency(backgroundCalls, "super.renderBackground") != 1) fail(file, "Native background must run exactly once.");
        for (String call : backgroundCalls) {
            if (call.equals("super.render") || (call.endsWith(".renderBackground") && !call.equals("super.renderBackground")))
                fail(file, "Late or repeated background/widget pass: " + call);
            if (call.endsWith(".renderTooltip") || call.endsWith(".renderComponentTooltip"))
                fail(file, "Tooltips must render after widgets, not in renderBackground.");
        }
        if (partScreen) {
            MethodTree render = methods.get("render");
            if (render == null) fail(file, "PartScreen needs a post-widget tooltip pass.");
            requireFirstCall(file, render, "super.render");
            List<String> renderCalls = calls(render);
            if (Collections.frequency(renderCalls, "super.render") != 1) fail(file, "Screen.render must be called exactly once.");
            if (renderCalls.stream().noneMatch(c -> c.endsWith(".renderTooltip"))) fail(file, "Missing post-widget row tooltip.");
            for (String call : renderCalls) {
                if (!call.equals("super.render") && !call.endsWith(".renderTooltip"))
                    fail(file, "Only row tooltips belong after widgets; found: " + call);
            }
            // Reset before drawing so moving off a row/changing tab cannot retain a stale tooltip.
            List<? extends StatementTree> statements = background.getBody().getStatements();
            if (statements.size() < 2 || !(statements.get(1) instanceof ExpressionStatementTree es)
                    || !(es.getExpression() instanceof AssignmentTree assignment)
                    || !assignment.getVariable().toString().equals("hoveredRowTooltip")
                    || assignment.getExpression().getKind() != Tree.Kind.NULL_LITERAL)
                fail(file, "Clear hoveredRowTooltip immediately after the native background pass.");
        } else if (methods.containsKey("render")) {
            fail(file, "GuideScreen must inherit Screen.render to retain background -> guide -> widgets ordering.");
        }
        System.out.println("PASS: " + file.getFileName());
    }

    private static void requireFirstCall(Path file, MethodTree method, String expected) {
        if (method.getBody() == null || method.getParameters().size() != 4 || method.getBody().getStatements().isEmpty())
            fail(file, "Expected four-parameter rendering method: " + method.getName());
        StatementTree statement = method.getBody().getStatements().getFirst();
        if (!(statement instanceof ExpressionStatementTree expression)
                || !(expression.getExpression() instanceof MethodInvocationTree call)
                || !call.getMethodSelect().toString().equals(expected) || call.getArguments().size() != 4)
            fail(file, method.getName() + " must begin with " + expected + "(graphics, mouseX, mouseY, partialTick).");
    }

    private static List<String> calls(MethodTree method) {
        List<String> result = new ArrayList<>();
        new TreeScanner<Void, Void>() {
            @Override public Void visitMethodInvocation(MethodInvocationTree call, Void unused) {
                result.add(call.getMethodSelect().toString());
                return super.visitMethodInvocation(call, unused);
            }
        }.scan(method.getBody(), null);
        return result;
    }
    private static void fail(Path file, String message) {
        throw new IllegalStateException(file.getFileName() + ": " + message);
    }
}
