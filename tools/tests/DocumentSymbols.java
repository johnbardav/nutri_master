import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.tools.*;
import com.sun.source.tree.*;
import com.sun.source.util.*;

/** Syntax-tree inventory; no Android class resolution or execution is claimed. */
public class DocumentSymbols {
    private static String clean(Object value) {
        return String.valueOf(value).replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }
    public static void main(String[] args) throws Exception {
        List<java.io.File> files = new ArrayList<>();
        try (java.util.stream.Stream<Path> stream = Files.walk(Paths.get(args[0]))) {
            stream.filter(p -> p.toString().endsWith(".java")).forEach(p -> files.add(p.toFile()));
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("A JDK is required, not a JRE.");
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager manager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            JavacTask task = (JavacTask) compiler.getTask(null, manager, diagnostics,
                Arrays.asList("--release", "17", "-proc:none"), null, manager.getJavaFileObjectsFromFiles(files));
            Trees trees = Trees.instance(task);
            for (CompilationUnitTree unit : task.parse()) {
                new TreePathScanner<Void, Void>() {
                    private String cls = "", method = "";
                    private void row(String kind, String name, String type, Tree node) {
                        long offset = trees.getSourcePositions().getStartPosition(unit, node);
                        long line = offset < 0 ? -1 : unit.getLineMap().getLineNumber(offset);
                        System.out.println(kind+"\t"+cls+"\t"+method+"\t"+clean(name)+"\t"+clean(type)+"\t"+line+"\t"+unit.getSourceFile().getName());
                    }
                    @Override public Void visitClass(ClassTree node, Void unused) {
                        String previous = cls;
                        cls = node.getSimpleName().length() == 0 ? previous + "$anonymous" : node.getSimpleName().toString();
                        super.visitClass(node, unused); cls = previous; return null;
                    }
                    @Override public Void visitMethod(MethodTree node, Void unused) {
                        String previous = method; method = node.getName().toString();
                        row("METHOD", method, node.getReturnType() == null ? "constructor" : node.getReturnType().toString(), node);
                        super.visitMethod(node, unused); method = previous; return null;
                    }
                    @Override public Void visitVariable(VariableTree node, Void unused) {
                        Tree parent = getCurrentPath().getParentPath().getLeaf();
                        row(parent instanceof ClassTree ? "FIELD" : parent instanceof MethodTree ? "PARAM" : "LOCAL",
                            node.getName().toString(), node.getType() == null ? "inferred" : node.getType().toString(), node);
                        return super.visitVariable(node, unused);
                    }
                    @Override public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
                        row("CALL", node.getMethodSelect().toString(), node.getArguments().toString(), node);
                        return super.visitMethodInvocation(node, unused);
                    }
                }.scan(unit, null);
            }
            for (Diagnostic<?> diagnostic : diagnostics.getDiagnostics()) {
                if (diagnostic.getKind() == Diagnostic.Kind.ERROR) throw new AssertionError(diagnostic.toString());
            }
        }
    }
}
