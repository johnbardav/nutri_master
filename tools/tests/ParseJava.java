import java.nio.file.*;
import java.util.*;
import javax.tools.*;
import com.sun.source.tree.*;
import com.sun.source.util.*;
/** AST checks only. NOT Android type checking, compilation or runtime testing. */
public class ParseJava {
    public static void main(String[] args)throws Exception {
        List<java.io.File> files=new ArrayList<>();
        try(java.util.stream.Stream<Path> paths=Files.walk(Paths.get(args[0]))) {
            paths.filter(p->p.toString().endsWith(".java")).forEach(p->files.add(p.toFile()));
        }
        JavaCompiler compiler=ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> d=new DiagnosticCollector<>();
        try(StandardJavaFileManager manager=compiler.getStandardFileManager(d,null,java.nio.charset.StandardCharsets.UTF_8)) {
            JavacTask task=(JavacTask)compiler.getTask(null,manager,d,Arrays.asList("--release","17","-proc:none"),null,manager.getJavaFileObjectsFromFiles(files));
            for(CompilationUnitTree unit:task.parse()) {
                new TreePathScanner<Void,Void>() {
                    @Override public Void visitReturn(ReturnTree node,Void unused) {
                        TreePath p=getCurrentPath().getParentPath();
                        while(p!=null && !(p.getLeaf() instanceof MethodTree)) {
                            if(p.getLeaf() instanceof LambdaExpressionTree)return super.visitReturn(node,unused);
                            p=p.getParentPath();
                        }
                        if(p!=null) {
                            MethodTree method=(MethodTree)p.getLeaf();
                            if(method.getReturnType()!=null && method.getReturnType().toString().equals("void") && node.getExpression()!=null)
                                throw new AssertionError(unit.getSourceFile()+": value returned from void method "+method.getName());
                        }
                        return super.visitReturn(node,unused);
                    }
                    @Override public Void visitMethod(MethodTree node,Void unused) {
                        if(node.getName().toString().equals("onCreateView") && node.getBody()!=null) {
                            List<? extends StatementTree> statements=node.getBody().getStatements();
                            if(statements.isEmpty() || !(statements.get(statements.size()-1) instanceof ReturnTree))
                                throw new AssertionError(unit.getSourceFile()+": onCreateView must return its view");
                        }
                        return super.visitMethod(node,unused);
                    }
                }.scan(unit,null);
            }
            for(Diagnostic<?> x:d.getDiagnostics())if(x.getKind()==Diagnostic.Kind.ERROR)throw new AssertionError(x.toString());
        }
        System.out.println("PASS: "+files.size()+" Java source files parsed and return statements checked. No Android type checking performed.");
    }
}
