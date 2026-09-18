import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

public class ConcatTransformer {

    public static void transformDirectory(String sourceDir, String outputDir) throws IOException {
        Path sourcePath = Paths.get(sourceDir);
        Path outputPath = Paths.get(outputDir);

        Files.createDirectories(outputPath);

        try (Stream<Path> paths = Files.walk(sourcePath)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> {
                        try {
                            transformFile(path, sourcePath, outputPath);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to transform: " + path, e);
                        }
                    });
        }
    }

    private static void transformFile(Path sourceFile, Path sourceRoot, Path outputRoot) throws IOException {
        String content = Files.readString(sourceFile);

        JavaParser javaParser = new JavaParser();
        // Configure parser to support Java 17 features (pattern matching with instanceof)
        javaParser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
        
        ParseResult<CompilationUnit> parseResult = javaParser.parse(content);

        if (!parseResult.isSuccessful()) {
            copyFile(sourceFile, sourceRoot, outputRoot);
            return;
        }

        CompilationUnit cu = parseResult.getResult().orElse(null);
        if (cu == null) {
            copyFile(sourceFile, sourceRoot, outputRoot);
            return;
        }

        // Remove all comments
        List<Comment> comments = cu.getAllComments();
        boolean commentsRemoved = !comments.isEmpty();
        comments.forEach(Comment::remove);

        boolean concatModified = transformConcatCalls(cu);

        Path relativePath = sourceRoot.relativize(sourceFile);
        Path outputFile = outputRoot.resolve(relativePath);

        Files.createDirectories(outputFile.getParent());

        if (concatModified || commentsRemoved) {
            try (FileWriter writer = new FileWriter(outputFile.toFile())) {
                writer.write(cu.toString());
            }
        } else {
            Files.copy(sourceFile, outputFile);
        }
    }

    private static boolean transformConcatCalls(CompilationUnit cu) {
        final boolean[] modified = {false};

        cu.findAll(MethodCallExpr.class).forEach(methodCall -> {
            if (transformConcatCall(methodCall)) {
                modified[0] = true;
            }
        });
        return modified[0];
    }

    public static boolean transformConcatCall(MethodCallExpr methodCall) {
        if (!isTarget(methodCall)) {
            return false;
        }

        int argCount = methodCall.getArguments().size();
        if (argCount != 1 || !(methodCall.getArgument(0) instanceof StringLiteralExpr stringArg)) {
            return false;
        }

        String value = stringArg.getValue();
        if (!shouldTransform(value)) {
            return false;
        }

        methodCall.getArguments().clear();
        for (char c : value.toCharArray()) {
            methodCall.addArgument(new StringLiteralExpr(String.valueOf(c)));
        }
        return true;
    }

    private static boolean shouldTransform(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }

        for (char c : value.toCharArray()) {

            if (!isBasicASCII(c)) {
                return false;
            }
        }

        return true;
    }

    private static boolean isBasicASCII(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                (c >= '0' && c <= '9') ||
                (c == ' ') ||
                (c == '.') ||
                (c == ',') ||
                (c == '!') ||
                (c == '?') ||
                (c == '-') ||
                (c == '_') ||
                (c == ':') ||
                (c == ';') ||
                (c == '\'') ||
                (c == '"');
    }

    private static boolean isTarget(MethodCallExpr methodCall) {
        if (!methodCall.getNameAsString().equals("of")) {
            return false;
        }

        return methodCall.getScope()
                .map(scope -> scope.toString().equals("Concat"))
                .orElse(false);
    }

    private static void copyFile(Path sourceFile, Path sourceRoot, Path outputRoot) throws IOException {
        Path relativePath = sourceRoot.relativize(sourceFile);
        Path outputFile = outputRoot.resolve(relativePath);
        Files.createDirectories(outputFile.getParent());
        Files.copy(sourceFile, outputFile);
    }
}
