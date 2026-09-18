package com.mixininject.agent;

import com.mixininject.api.Inject;
import com.mixininject.api.Mixin;
import com.mixininject.api.Overwrite;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/* Scans mixin class bytes for @Mixin/@Inject/@Overwrite and indexes them by target class,
 * then dispatches transforms through MixinApplicator. */
public final class MixinRegistry {
   private static final int API = Opcodes.ASM9;
   private final Map<String, List<MixinInfo>> byTarget = new LinkedHashMap<>();
   private final List<MixinInfo> all = new ArrayList<>();
   private final Logger log;
   private volatile boolean frozen = false;

   public MixinRegistry(Logger log) {
      this.log = log == null ? msg -> {} : log;
   }

   public synchronized void register(byte[] classBytes) {
      this.register(classBytes, "unknown");
   }

   public synchronized void register(byte[] classBytes, String source) {
      ClassReader reader = new ClassReader(classBytes);
      String mixinName = reader.getClassName().replace('/', '.');
      Scanner scanner = new Scanner(mixinName);
      reader.accept(scanner, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
      if (scanner.targets.isEmpty()) {
         return;
      }
      for (String target : scanner.targets) {
         MixinInfo info = new MixinInfo(target, mixinName, classBytes);
         info.injections.addAll(scanner.injections);
         this.byTarget.computeIfAbsent(info.targetInternalName, k -> new ArrayList<>()).add(info);
         this.all.add(info);
         this.log.log("[mixin] registered " + mixinName + " -> " + target
                    + " (" + info.injections.size() + " transforms) from " + source);
      }
   }

   public void freeze() {
      this.frozen = true;
   }

   public boolean isFrozen() {
      return this.frozen;
   }

   public synchronized List<MixinInfo> mixinsFor(String targetInternal) {
      return this.byTarget.getOrDefault(targetInternal, Collections.emptyList());
   }

   public synchronized List<MixinInfo> allMixins() {
      return Collections.unmodifiableList(this.all);
   }

   /** Dotted names of every target class touched by a mixin. */
   public synchronized List<String> targetClasses() {
      ArrayList<String> out = new ArrayList<>();
      for (List<MixinInfo> group : this.byTarget.values()) {
         if (!group.isEmpty()) {
            out.add(group.get(0).targetClassName);
         }
      }
      return out;
   }

   public byte[] transform(String dottedName, byte[] classBytes) {
      return this.transform(dottedName, classBytes, null);
   }

   public byte[] transform(String dottedName, byte[] classBytes, ClassLoader loader) {
      if (classBytes == null) {
         return null;
      }
      String internal = dottedName.replace('.', '/');
      List<MixinInfo> mixins;
      synchronized (this) {
         mixins = this.byTarget.get(internal);
      }
      if (mixins == null || mixins.isEmpty()) {
         return classBytes;
      }
      try {
         return MixinApplicator.apply(classBytes, mixins, this.log, loader);
      } catch (Throwable t) {
         this.log.log("[mixin] FAILED to transform " + dottedName + ": " + t);
         t.printStackTrace();
         return classBytes;
      }
   }

   public interface Logger {
      void log(String message);
   }

   /* Reads @Mixin targets off the class + @Inject/@Overwrite off each method. */
   private static final class Scanner extends ClassVisitor {
      final List<String> targets = new ArrayList<>();
      final List<InjectionPoint> injections = new ArrayList<>();
      private final String mixinName;

      Scanner(String mixinName) {
         super(API);
         this.mixinName = mixinName;
      }

      @Override
      public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
         if (!Type.getDescriptor(Mixin.class).equals(desc)) {
            return null;
         }
         return new AnnotationVisitor(API) {
            @Override
            public AnnotationVisitor visitArray(String name) {
               if (!"value".equals(name) && !"targets".equals(name)) {
                  return null;
               }
               return new AnnotationVisitor(API) {
                  @Override
                  public void visit(String n, Object value) {
                     if (value instanceof String s) {
                        Scanner.this.targets.add(s);
                     } else if (value instanceof Type t) {
                        Scanner.this.targets.add(t.getInternalName().replace('/', '.'));
                     }
                  }
               };
            }
         };
      }

      @Override
      public MethodVisitor visitMethod(int access, final String methodName, final String methodDesc,
                                       String signature, String[] exceptions) {
         return new MethodVisitor(API) {
            private String injMethod;
            private String injDesc = "";
            private String injAt = "HEAD";
            private boolean captureArgs = false;
            private boolean captureReturn = false;
            private boolean cancellable = false;
            private String owMethod = "";
            private String owDesc = "";
            private boolean isInject = false;
            private boolean isOverwrite = false;

            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
               if (Type.getDescriptor(Inject.class).equals(desc)) {
                  this.isInject = true;
                  return new AnnotationVisitor(API) {
                     @Override
                     public void visit(String name, Object value) {
                        switch (name) {
                           case "method"        -> injMethod = (String) value;
                           case "desc"          -> injDesc = (String) value;
                           case "at"            -> injAt = (String) value;
                           case "captureArgs"   -> captureArgs = (Boolean) value;
                           case "captureReturn" -> captureReturn = (Boolean) value;
                           case "cancellable"   -> cancellable = (Boolean) value;
                        }
                     }
                  };
               } else if (Type.getDescriptor(Overwrite.class).equals(desc)) {
                  this.isOverwrite = true;
                  return new AnnotationVisitor(API) {
                     @Override
                     public void visit(String name, Object value) {
                        switch (name) {
                           case "method" -> owMethod = (String) value;
                           case "desc"   -> owDesc = (String) value;
                        }
                     }
                  };
               }
               return null;
            }

            @Override
            public void visitEnd() {
               if (this.isInject) {
                  String target = this.injMethod != null && !this.injMethod.isEmpty() ? this.injMethod : methodName;
                  String targetDesc = this.injDesc == null ? "" : this.injDesc;
                  InjectionPoint.Kind kind = "RETURN".equalsIgnoreCase(this.injAt)
                      ? InjectionPoint.Kind.INJECT_RETURN : InjectionPoint.Kind.INJECT_HEAD;
                  Scanner.this.injections.add(new InjectionPoint(kind, Scanner.this.mixinName,
                      methodName, methodDesc, target, targetDesc,
                      this.captureArgs, this.captureReturn, this.cancellable));
               } else if (this.isOverwrite) {
                  String target = this.owMethod != null && !this.owMethod.isEmpty() ? this.owMethod : methodName;
                  String targetDesc = this.owDesc != null && !this.owDesc.isEmpty() ? this.owDesc : methodDesc;
                  Scanner.this.injections.add(new InjectionPoint(InjectionPoint.Kind.OVERWRITE, Scanner.this.mixinName,
                      methodName, methodDesc, target, targetDesc, false, false, false));
               }
            }
         };
      }
   }
}
