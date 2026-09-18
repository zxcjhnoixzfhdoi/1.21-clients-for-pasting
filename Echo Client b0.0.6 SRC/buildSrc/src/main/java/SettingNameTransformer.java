import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.*;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * Global Strings Transformer
 * 
 * 1. Scans ALL Java files for Concat.of() calls
 * 2. Collects all strings into a global strings array (with deduplication)
 * 3. Replaces Concat.of() with CentralFeatureInfoHub.getGlobalString(index) calls
 * 4. Generates features.json with globalStrings array for the backend
 * 
 * Excludes single unicode escape sequences (icon characters like \uE2B4)
 * 
 * @author Cyde
 */
public class SettingNameTransformer {
    
    // Global strings list - maintains insertion order, uses map for deduplication
    private static final List<String> globalStrings = new ArrayList<>();
    private static final Map<String, Integer> stringToIndex = new LinkedHashMap<>();
    
    // Track which files need transformation
    private static final Map<Path, List<TransformInfo>> fileTransformations = new LinkedHashMap<>();
    
    // Info about a single transformation to make
    private static class TransformInfo {
        final int globalStringIndex;
        final MethodCallExpr originalExpr;
        
        TransformInfo(int globalStringIndex, MethodCallExpr originalExpr) {
            this.globalStringIndex = globalStringIndex;
            this.originalExpr = originalExpr;
        }
    }
    
    /**
     * Main entry point - transforms all Concat.of() calls to global string references
     */
    public static void transform(String sourceDir, String featuresJsonPath) throws IOException {
        Path sourcePath = Paths.get(sourceDir);
        
        // Clear previous state
        globalStrings.clear();
        stringToIndex.clear();
        fileTransformations.clear();
        
        // Phase 1: Discovery - scan ALL Java files and collect strings
        System.out.println("[GlobalStringsTransformer] Phase 1: Discovering all Concat.of() calls...");
        discoverAllStrings(sourcePath);
        System.out.println("[GlobalStringsTransformer] Discovered " + globalStrings.size() + " unique strings");
        
        // Phase 2: Transform - replace Concat.of() with getGlobalString() references
        System.out.println("[GlobalStringsTransformer] Phase 2: Transforming sources...");
        transformAllSources();
        
        // Phase 3: Generate features.json with globalStrings array
        System.out.println("[GlobalStringsTransformer] Phase 3: Generating features.json...");
        generateFeaturesJson(featuresJsonPath);
        
        System.out.println("[GlobalStringsTransformer] Don't forget to upload the new features.json to the server!");
        System.out.println("[GlobalStringsTransformer] If you don't, you will get a mismatch and crash!");
        System.out.println("[GlobalStringsTransformer] Transformation complete!");
    }
    
    /**
     * Phase 1: Walk all Java files and discover Concat.of() calls
     */
    private static void discoverAllStrings(Path sourceRoot) throws IOException {
        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".java"))
                 .sorted() // Ensure consistent ordering
                 .forEach(SettingNameTransformer::discoverStringsInFile);
        }
    }
    
    private static void discoverStringsInFile(Path filePath) {
        try {
            String content = Files.readString(filePath);
            JavaParser parser = new JavaParser();
            parser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
            
            ParseResult<CompilationUnit> result = parser.parse(content);
            if (!result.isSuccessful() || result.getResult().isEmpty()) {
                return;
            }
            
            CompilationUnit cu = result.getResult().get();
            List<TransformInfo> transforms = new ArrayList<>();
            
            // Find all Concat.of() method calls
            cu.findAll(MethodCallExpr.class).forEach(methodCall -> {
                if (!isConcatOfCall(methodCall)) return;

                // If this call has dynamic arguments, skip global-string transform.
                // It will be handled by ConcatTransformer fallback during phase 2.
                if (!isServerTransformableConcatCall(methodCall)) return;
                
                // Extract the string value
                String value = extractStringFromConcatOf(methodCall);
                if (value == null || value.isEmpty()) return;
                
                // Skip strings containing unicode icons (like \uE2B4)
                if (containsUnicodeIcons(value)) {
                    System.out.println("[GlobalStringsTransformer] Skipping unicode icon: " + Integer.toHexString(value.charAt(0)));
                    return;
                }
                
                // Get or assign index for this string
                int index = getOrAssignIndex(value);
                transforms.add(new TransformInfo(index, methodCall));
            });
            
            if (!transforms.isEmpty()) {
                fileTransformations.put(filePath, transforms);
            }
            
        } catch (Exception e) {
            System.err.println("[GlobalStringsTransformer] Error processing " + filePath + ": " + e.getMessage());
        }
    }
    
    /**
     * Check if this is a Concat.of() method call
     */
    private static boolean isConcatOfCall(MethodCallExpr methodCall) {
        if (!methodCall.getNameAsString().equals("of")) {
            return false;
        }
        return methodCall.getScope()
            .map(scope -> scope.toString().equals("Concat"))
            .orElse(false);
    }

    private static boolean isServerTransformableConcatCall(MethodCallExpr methodCall) {
        if (methodCall.getArguments().isEmpty()) {
            return false;
        }
        for (Expression arg : methodCall.getArguments()) {
            if (!(arg instanceof StringLiteralExpr)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Extract the string value from a Concat.of() call
     * Handles both Concat.of("string") and Concat.of("s", "t", "r", ...)
     */
    private static String extractStringFromConcatOf(MethodCallExpr methodCall) {
        StringBuilder sb = new StringBuilder();
        for (Expression arg : methodCall.getArguments()) {
            if (arg instanceof StringLiteralExpr strLit) {
                sb.append(strLit.getValue());
            }
        }
        return sb.toString();
    }
    
    /**
     * Check if string contains unicode icons that should NOT be transformed.
     * These are icon characters in Private Use Area (E000-F8FF) or other special ranges.
     * Examples: \uE2B4, \uE06D, etc.
     * 
     */
    private static boolean containsUnicodeIcons(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        
        // Check for literal unicode escape sequences like \uE2B4 (returned as string by JavaParser)
        // Pattern: backslash + u + 4 hex digits in PUA range (E000-F8FF)
        if (value.matches(".*\\\\u[EeFf][0-9A-Fa-f]{3}.*")) {
            return true;
        }
        
        // Also check for decoded unicode characters
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            // Private Use Area: E000-F8FF
            if (c >= 0xE000 && c <= 0xF8FF) {
                return true;
            }
            // Supplementary Private Use Area and other high unicode
            if (c >= 0xD800 && c <= 0xDFFF) {
                return true; // Surrogate pairs (for characters > U+FFFF)
            }
        }
        return false;
    }
    
    /**
     * Get existing index or assign new one for the string
     */
    private static int getOrAssignIndex(String value) {
        Integer existingIndex = stringToIndex.get(value);
        if (existingIndex != null) {
            return existingIndex;
        }
        
        int newIndex = globalStrings.size();
        globalStrings.add(value);
        stringToIndex.put(value, newIndex);
        return newIndex;
    }
    
    /**
     * Phase 2: Transform all files that have Concat.of() calls
     */
    private static void transformAllSources() throws IOException {
        for (Map.Entry<Path, List<TransformInfo>> entry : fileTransformations.entrySet()) {
            transformFile(entry.getKey(), entry.getValue());
        }
    }
    
    private static void transformFile(Path filePath, List<TransformInfo> transforms) throws IOException {
        String content = Files.readString(filePath);
        
        JavaParser parser = new JavaParser();
        parser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
        
        ParseResult<CompilationUnit> result = parser.parse(content);
        if (!result.isSuccessful() || result.getResult().isEmpty()) {
            return;
        }
        
        CompilationUnit cu = result.getResult().get();
        boolean modified = false;
        
        // Build a set of expressions to transform (by reference)
        Set<MethodCallExpr> toTransform = new HashSet<>();
        Map<MethodCallExpr, Integer> exprToIndex = new HashMap<>();
        for (TransformInfo info : transforms) {
            toTransform.add(info.originalExpr);
            exprToIndex.put(info.originalExpr, info.globalStringIndex);
        }
        
        // Find and transform all matching Concat.of() calls
        List<MethodCallExpr> allCalls = cu.findAll(MethodCallExpr.class);
        for (MethodCallExpr methodCall : allCalls) {
            if (!isConcatOfCall(methodCall)) continue;

            // if global-string transform is not safe for this call (aka not a string literal)
            // pass it to ConcatTransformer behavior instead.
            if (!isServerTransformableConcatCall(methodCall)) {
                if (ConcatTransformer.transformConcatCall(methodCall)) {
                    modified = true;
                }
                continue;
            }
            
            String value = extractStringFromConcatOf(methodCall);
            if (value == null || value.isEmpty()) continue;
            if (containsUnicodeIcons(value)) continue;
            
            // Get the index for this string
            Integer index = stringToIndex.get(value);
            if (index == null) continue;
            
            // Replace with CentralFeatureInfoHub.getGlobalString(index)
            MethodCallExpr replacement = createGetGlobalStringCall(index);
            methodCall.replace(replacement);
            modified = true;
        }
        
        if (modified) {
            // Ensure CentralFeatureInfoHub import exists
            boolean hasHubImport = cu.getImports().stream()
                .anyMatch(i -> i.getNameAsString().contains("CentralFeatureInfoHub"));
            
            if (!hasHubImport) {
                cu.addImport("hack.echo.client.auth.CentralFeatureInfoHub");
            }
            
            try (FileWriter writer = new FileWriter(filePath.toFile())) {
                writer.write(cu.toString());
            }
            System.out.println("[GlobalStringsTransformer] Transformed: " + filePath.getFileName());
        }
    }
    
    /**
     * Creates: CentralFeatureInfoHub.getGlobalString(index)
     */
    private static MethodCallExpr createGetGlobalStringCall(int index) {
        return new MethodCallExpr(
            new NameExpr("CentralFeatureInfoHub"),
            "getGlobalString",
            new com.github.javaparser.ast.NodeList<>(new IntegerLiteralExpr(String.valueOf(index)))
        );
    }
    
    /**
     * Phase 3: Generate features.json with globalStrings array
     */
    private static void generateFeaturesJson(String outputPath) throws IOException {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        
        // Write globalStrings array
        json.append("  \"globalStrings\": [\n");
        for (int i = 0; i < globalStrings.size(); i++) {
            json.append("    \"").append(escapeJson(globalStrings.get(i))).append("\"");
            if (i < globalStrings.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        json.append("  ]\n");
        
        json.append("}\n");
        
        Files.writeString(Paths.get(outputPath), json.toString());
        System.out.println("[GlobalStringsTransformer] Generated: " + outputPath + " with " + globalStrings.size() + " strings");
    }
    
    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
