package vlt;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.type.PrimitiveType;

import java.util.Optional;

/**
 * Detects whether an expression requires a compile-time constant,
 * meaning it cannot be replaced with a method call.
 */
public class CompileTimeConstantDetector {

    public static boolean requiresCompileTimeConstant(Expression expr) {
        // 1. Switch case labels
        if (isInSwitchLabel(expr)) return true;

        // 2. Annotations
        if (hasAncestorOfType(expr, AnnotationExpr.class)) return true;

        // 3. static final field initializers
        if (isStaticFinalFieldInit(expr)) return true;

        // 4. Enum constant declarations
        if (hasAncestorOfType(expr, EnumConstantDeclaration.class)) return true;

        // 5. byte/short variable declarations (narrowing not allowed from method return)
        if (isByteOrShortAssignment(expr)) return true;

        // 6. Index args in existing CentralFeatureInfoHub.getGlobal*() calls
        if (isHubGetterArg(expr)) return true;

        return false;
    }

    public static boolean isInSwitchLabel(Expression expr) {
        Optional<Node> parent = expr.getParentNode();
        // Direct parent might be UnaryExpr for negative literals
        if (parent.isPresent() && parent.get() instanceof UnaryExpr) {
            parent = parent.get().getParentNode();
        }
        if (parent.isPresent() && parent.get() instanceof SwitchEntry switchEntry) {
            return switchEntry.getLabels().contains(expr) ||
                   switchEntry.getLabels().stream().anyMatch(l ->
                       l instanceof UnaryExpr u && u.getExpression() == expr);
        }
        return false;
    }

    public static boolean isStaticFinalFieldInit(Expression expr) {
        Optional<Node> parent = expr.getParentNode();
        // Walk up past UnaryExpr
        Node current = expr;
        while (parent.isPresent() && parent.get() instanceof UnaryExpr) {
            current = parent.get();
            parent = current.getParentNode();
        }
        if (parent.isPresent() && parent.get() instanceof VariableDeclarator vd) {
            Optional<Node> fieldParent = vd.getParentNode();
            if (fieldParent.isPresent() && fieldParent.get() instanceof FieldDeclaration fd) {
                return fd.isStatic() && fd.isFinal();
            }
        }
        return false;
    }

    public static boolean isByteOrShortAssignment(Expression expr) {
        Optional<Node> parent = expr.getParentNode();
        Node current = expr;
        while (parent.isPresent() && parent.get() instanceof UnaryExpr) {
            current = parent.get();
            parent = current.getParentNode();
        }
        if (parent.isPresent() && parent.get() instanceof VariableDeclarator vd) {
            if (vd.getType() instanceof PrimitiveType pt) {
                PrimitiveType.Primitive type = pt.getType();
                return type == PrimitiveType.Primitive.BYTE || type == PrimitiveType.Primitive.SHORT;
            }
        }
        return false;
    }

    public static boolean isHubGetterArg(Expression expr) {
        Optional<Node> parent = expr.getParentNode();
        if (parent.isPresent() && parent.get() instanceof MethodCallExpr mce) {
            String name = mce.getNameAsString();
            if (name.startsWith("getGlobal")) {
                return mce.getScope()
                    .map(s -> s.toString().equals("CentralFeatureInfoHub"))
                    .orElse(false);
            }
        }
        return false;
    }

    public static <T extends Node> boolean hasAncestorOfType(Node node, Class<T> type) {
        Optional<Node> parent = node.getParentNode();
        while (parent.isPresent()) {
            if (type.isInstance(parent.get())) return true;
            parent = parent.get().getParentNode();
        }
        return false;
    }
}
