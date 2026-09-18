import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.*;
import json.FeaturesJsonMerger;
import vlt.CompileTimeConstantDetector;
import vlt.LiteralEntry;
import vlt.LiteralParser;
import vlt.LiteralType;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * Value Literal Transformer
 * <p>
 * 1. Scans ALL Java files for int, float, double, long, and boolean literals
 * 2. Collects all literals, shuffles them, and assigns randomized indices
 * 3. Replaces literals with CentralFeatureInfoHub.getGlobal*(index) calls
 * 4. Merges new arrays into existing features.json
 * <p>
 * @author Cyde
 */
public class ValueLiteralTransformer {

    // Collected literals (Phase 1) and lookup map (Phase 1.5)
    private static final List<LiteralEntry> allLiterals = new ArrayList<>();
    // (filePath, literalType, fileLocalOrder) -> assigned shuffled index
    private static final Map<Path, Map<LiteralType, Map<Integer, Integer>>> indexLookup = new HashMap<>();

    // Per-type arrays (populated after shuffle)
    private static final List<Integer> globalInts = new ArrayList<>();
    private static final List<Float> globalFloats = new ArrayList<>();
    private static final List<Double> globalDoubles = new ArrayList<>();
    private static final List<Long> globalLongs = new ArrayList<>();
    private static final List<Boolean> globalBooleans = new ArrayList<>();

    // Stats
    private static int totalIntLiterals = 0;
    private static int totalFloatLiterals = 0;
    private static int totalDoubleLiterals = 0;
    private static int totalLongLiterals = 0;
    private static int totalBooleanLiterals = 0;
    private static int skippedCompileTime = 0;

    // Files that should be completely excluded from transformation
    // Likely different in prod codebase
    private static final Set<String> EXCLUDED_FILES = Set.of(
        "CentralFeatureInfoHub.java",
        "MathProt.java",
        "AuthManager.java",
        "AuthHandler.java"
    );

    /**
     * Main entry point
     */
    public static void transform(String sourceDir, String featuresJsonPath) throws IOException {
        Path sourcePath = Paths.get(sourceDir);

        // Clear previous state
        clearState();

        // Phase 1: Collect all literals
        System.out.println("[ValueLiteralTransformer] Phase 1: Collecting all value literals...");
        List<Path> javaFiles = collectJavaFiles(sourcePath);
        for (Path file : javaFiles) {
            collectLiteralsInFile(file);
        }
        printDiscoveryStats();

        // Phase 1.5: Shuffle and assign indices
        System.out.println("[ValueLiteralTransformer] Phase 1.5: Shuffling and assigning indices...");
        shuffleAndAssignIndices();

        // Phase 2: Transform
        System.out.println("[ValueLiteralTransformer] Phase 2: Transforming sources...");
        for (Path file : javaFiles) {
            transformFile(file);
        }

        // Phase 3: Merge into features.json
        System.out.println("[ValueLiteralTransformer] Phase 3: Merging into features.json...");
        FeaturesJsonMerger.mergeFeaturesJson(featuresJsonPath, globalInts, globalFloats, globalDoubles, globalLongs, globalBooleans);

        System.out.println("[ValueLiteralTransformer] Transformation complete!");
    }

    private static void clearState() {
        allLiterals.clear();
        indexLookup.clear();
        globalInts.clear();
        globalFloats.clear();
        globalDoubles.clear();
        globalLongs.clear();
        globalBooleans.clear();
        totalIntLiterals = 0;
        totalFloatLiterals = 0;
        totalDoubleLiterals = 0;
        totalLongLiterals = 0;
        totalBooleanLiterals = 0;
        skippedCompileTime = 0;
    }

    private static List<Path> collectJavaFiles(Path sourceRoot) throws IOException {
        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            return paths.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".java"))
                        .filter(ValueLiteralTransformer::shouldProcessFile)
                        .sorted()
                        .toList();
        }
    }

    private static boolean shouldProcessFile(Path filePath) {
        String fileName = filePath.getFileName().toString();
        if (EXCLUDED_FILES.contains(fileName)) {
            return false;
        }
        // Skip mixin packages
        String pathStr = filePath.toString().replace('\\', '/');
        return !pathStr.contains("/mixin/") && !pathStr.contains("\\mixin\\");
    }

    // =========================================================================
    // Phase 1: Collect all literals
    // =========================================================================

    private static void collectLiteralsInFile(Path filePath) {
        try {
            String content = Files.readString(filePath);
            JavaParser parser = new JavaParser();
            parser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);

            ParseResult<CompilationUnit> result = parser.parse(content);
            if (!result.isSuccessful() || result.getResult().isEmpty()) {
                return;
            }

            CompilationUnit cu = result.getResult().get();

            // Per-type counters for file-local ordering
            int[] intOrder = {0};
            int[] floatOrder = {0};
            int[] doubleOrder = {0};
            int[] longOrder = {0};
            int[] booleanOrder = {0};

            // Integer literals
            cu.findAll(IntegerLiteralExpr.class).forEach(expr -> {
                if (CompileTimeConstantDetector.requiresCompileTimeConstant(expr)) {
                    skippedCompileTime++;
                    return;
                }
                int value = LiteralParser.parseIntLiteral(expr.getValue());
                allLiterals.add(new LiteralEntry(LiteralType.INT, value, filePath, intOrder[0]++));
                totalIntLiterals++;
            });

            // Double literals (includes float suffix)
            cu.findAll(DoubleLiteralExpr.class).forEach(expr -> {
                if (CompileTimeConstantDetector.requiresCompileTimeConstant(expr)) {
                    skippedCompileTime++;
                    return;
                }
                String raw = expr.getValue();
                if (LiteralParser.isFloatLiteral(raw)) {
                    float value = LiteralParser.parseFloatLiteral(raw);
                    allLiterals.add(new LiteralEntry(LiteralType.FLOAT, value, filePath, floatOrder[0]++));
                    totalFloatLiterals++;
                } else {
                    double value = LiteralParser.parseDoubleLiteral(raw);
                    allLiterals.add(new LiteralEntry(LiteralType.DOUBLE, value, filePath, doubleOrder[0]++));
                    totalDoubleLiterals++;
                }
            });

            // Long literals
            cu.findAll(LongLiteralExpr.class).forEach(expr -> {
                if (CompileTimeConstantDetector.requiresCompileTimeConstant(expr)) {
                    skippedCompileTime++;
                    return;
                }
                long value = LiteralParser.parseLongLiteral(expr.getValue());
                allLiterals.add(new LiteralEntry(LiteralType.LONG, value, filePath, longOrder[0]++));
                totalLongLiterals++;
            });

            // Boolean literals
            cu.findAll(BooleanLiteralExpr.class).forEach(expr -> {
                if (CompileTimeConstantDetector.requiresCompileTimeConstant(expr)) {
                    skippedCompileTime++;
                    return;
                }
                allLiterals.add(new LiteralEntry(LiteralType.BOOLEAN, expr.getValue(), filePath, booleanOrder[0]++));
                totalBooleanLiterals++;
            });

        } catch (Exception e) {
            System.err.println("[ValueLiteralTransformer] Error collecting " + filePath + ": " + e.getMessage());
        }
    }

    // =========================================================================
    // Phase 1.5: Shuffle and assign indices
    // =========================================================================

    private static void shuffleAndAssignIndices() {
        // Shuffle with unseeded Random so indices differ each build
        Collections.shuffle(allLiterals, new Random());

        // Assign indices per type from shuffled order and populate global arrays + lookup map
        int intIdx = 0, floatIdx = 0, doubleIdx = 0, longIdx = 0, booleanIdx = 0;

        for (LiteralEntry entry : allLiterals) {
            int assignedIndex;
            switch (entry.type()) {
                case INT -> {
                    assignedIndex = intIdx++;
                    globalInts.add((Integer) entry.value());
                }
                case FLOAT -> {
                    assignedIndex = floatIdx++;
                    globalFloats.add((Float) entry.value());
                }
                case DOUBLE -> {
                    assignedIndex = doubleIdx++;
                    globalDoubles.add((Double) entry.value());
                }
                case LONG -> {
                    assignedIndex = longIdx++;
                    globalLongs.add((Long) entry.value());
                }
                case BOOLEAN -> {
                    assignedIndex = booleanIdx++;
                    globalBooleans.add((Boolean) entry.value());
                }
                default -> throw new IllegalStateException("Unknown literal type: " + entry.type());
            }

            // Store in lookup: (filePath, type, fileLocalOrder) --> index
            indexLookup
                .computeIfAbsent(entry.filePath(), k -> new HashMap<>())
                .computeIfAbsent(entry.type(), k -> new HashMap<>())
                .put(entry.fileLocalOrder(), assignedIndex);
        }

        System.out.println("[ValueLiteralTransformer] Shuffled " + allLiterals.size() + " literals across all types");
    }

    // =========================================================================
    // Phase 2: Transformation
    // =========================================================================

    private static void transformFile(Path filePath) {
        try {
            // Get the lookup for this file, skips if no literals were collected
            Map<LiteralType, Map<Integer, Integer>> fileLookup = indexLookup.get(filePath);
            if (fileLookup == null) return;

            String content = Files.readString(filePath);
            JavaParser parser = new JavaParser();
            parser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);

            ParseResult<CompilationUnit> result = parser.parse(content);
            if (!result.isSuccessful() || result.getResult().isEmpty()) {
                return;
            }

            CompilationUnit cu = result.getResult().get();
            boolean modified = false;

            // Per-type counters to match file-local discovery order
            int intOrder = 0;
            int floatOrder = 0;
            int doubleOrder = 0;
            int longOrder = 0;
            int booleanOrder = 0;

            // Transform integer literals
            Map<Integer, Integer> intMap = fileLookup.getOrDefault(LiteralType.INT, Map.of());
            List<IntegerLiteralExpr> intExprs = new ArrayList<>(cu.findAll(IntegerLiteralExpr.class));
            for (IntegerLiteralExpr expr : intExprs) {
                if (CompileTimeConstantDetector.requiresCompileTimeConstant(expr)) continue;
                Integer index = intMap.get(intOrder++);
                if (index == null) continue;
                expr.replace(createGetterCall("getGlobalInt", index));
                modified = true;
            }

            // Transform double literals (float + double)
            Map<Integer, Integer> floatMap = fileLookup.getOrDefault(LiteralType.FLOAT, Map.of());
            Map<Integer, Integer> doubleMap = fileLookup.getOrDefault(LiteralType.DOUBLE, Map.of());
            List<DoubleLiteralExpr> doubleExprs = new ArrayList<>(cu.findAll(DoubleLiteralExpr.class));
            for (DoubleLiteralExpr expr : doubleExprs) {
                if (CompileTimeConstantDetector.requiresCompileTimeConstant(expr)) continue;
                String raw = expr.getValue();
                if (LiteralParser.isFloatLiteral(raw)) {
                    Integer index = floatMap.get(floatOrder++);
                    if (index == null) continue;
                    expr.replace(createGetterCall("getGlobalFloat", index));
                } else {
                    Integer index = doubleMap.get(doubleOrder++);
                    if (index == null) continue;
                    expr.replace(createGetterCall("getGlobalDouble", index));
                }
                modified = true;
            }

            // Transform long literals
            Map<Integer, Integer> longMap = fileLookup.getOrDefault(LiteralType.LONG, Map.of());
            List<LongLiteralExpr> longExprs = new ArrayList<>(cu.findAll(LongLiteralExpr.class));
            for (LongLiteralExpr expr : longExprs) {
                if (CompileTimeConstantDetector.requiresCompileTimeConstant(expr)) continue;
                Integer index = longMap.get(longOrder++);
                if (index == null) continue;
                expr.replace(createGetterCall("getGlobalLong", index));
                modified = true;
            }

            // Transform boolean literals
            Map<Integer, Integer> boolMap = fileLookup.getOrDefault(LiteralType.BOOLEAN, Map.of());
            List<BooleanLiteralExpr> boolExprs = new ArrayList<>(cu.findAll(BooleanLiteralExpr.class));
            for (BooleanLiteralExpr expr : boolExprs) {
                if (CompileTimeConstantDetector.requiresCompileTimeConstant(expr)) continue;
                Integer index = boolMap.get(booleanOrder++);
                if (index == null) continue;
                expr.replace(createGetterCall("getGlobalBoolean", index));
                modified = true;
            }

            if (modified) {
                // Ensure import exists
                boolean hasHubImport = cu.getImports().stream()
                    .anyMatch(i -> i.getNameAsString().contains("CentralFeatureInfoHub"));
                if (!hasHubImport) {
                    cu.addImport("hack.echo.client.auth.CentralFeatureInfoHub");
                }

                try (FileWriter writer = new FileWriter(filePath.toFile())) {
                    writer.write(cu.toString());
                }
                System.out.println("[ValueLiteralTransformer] Transformed: " + filePath.getFileName());
            }

        } catch (Exception e) {
            System.err.println("[ValueLiteralTransformer] Error transforming " + filePath + ": " + e.getMessage());
        }
    }

    // =========================================================================
    // Getter call creation
    // =========================================================================

    private static MethodCallExpr createGetterCall(String methodName, int index) {
        return new MethodCallExpr(
            new NameExpr("CentralFeatureInfoHub"),
            methodName,
            new com.github.javaparser.ast.NodeList<>(new IntegerLiteralExpr(String.valueOf(index)))
        );
    }

    // =========================================================================
    // Stats
    // =========================================================================

    private static void printDiscoveryStats() {
        System.out.println("[ValueLiteralTransformer] Collected " + totalIntLiterals + " int literals");
        System.out.println("[ValueLiteralTransformer] Collected " + totalFloatLiterals + " float literals");
        System.out.println("[ValueLiteralTransformer] Collected " + totalDoubleLiterals + " double literals");
        System.out.println("[ValueLiteralTransformer] Collected " + totalLongLiterals + " long literals");
        System.out.println("[ValueLiteralTransformer] Collected " + totalBooleanLiterals + " boolean literals");
        System.out.println("[ValueLiteralTransformer] Collected " + allLiterals.size() + " total literals");
        System.out.println("[ValueLiteralTransformer] Skipped " + skippedCompileTime + " literals in compile-time constant contexts");
    }
}
