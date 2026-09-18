package com.mixininject.agent;

import com.mixininject.agent.MixinRegistry.Logger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.MethodRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/* Applies @Overwrite / @Inject(HEAD|RETURN) mixins onto a target class's bytecode.
 * Handler method bodies are remapped (self-refs -> target, local slots rebased) and
 * spliced in; @cancellable handlers get a Cancel.has()/consume() short-circuit woven
 * in after HEAD or before each RETURN. */
final class MixinApplicator {
   private static final int API = Opcodes.ASM9;
   private static final String CANCEL = "com/mixininject/api/Cancel";
   private static final ThreadLocal<ClassLoader> LOADER = new ThreadLocal<>();

   static byte[] apply(byte[] targetBytes, List<MixinInfo> mixins, Logger log) {
      return apply(targetBytes, mixins, log, null);
   }

   static byte[] apply(byte[] targetBytes, List<MixinInfo> mixins, Logger log, ClassLoader loader) {
      ClassLoader prev = LOADER.get();
      if (loader != null) {
         LOADER.set(loader);
      }
      try {
         return doApply(targetBytes, mixins, log);
      } finally {
         if (loader != null) {
            if (prev != null) {
               LOADER.set(prev);
            } else {
               LOADER.remove();
            }
         }
      }
   }

   private static byte[] doApply(byte[] targetBytes, List<MixinInfo> mixins, Logger log) {
      ClassReader reader = new ClassReader(targetBytes);
      ClassNode target = new ClassNode(API);
      reader.accept(target, 0);
      final String targetName = target.name;

      for (final MixinInfo mixin : mixins) {
         ClassNode mixinNode = new ClassNode(API);
         new ClassReader(mixin.mixinBytes).accept(mixinNode, 0);
         Remapper selfToTarget = new Remapper() {
            @Override
            public String map(String internalName) {
               return internalName.equals(mixin.targetInternalName) ? targetName : internalName;
            }
         };

         for (InjectionPoint point : mixin.injections) {
            MethodNode handler = findMethod(mixinNode, point.mixinMethodName, point.mixinMethodDesc);
            if (handler == null) {
               log.log("[mixin] " + mixin.targetClassName + ": no method " + point.mixinMethodName + point.mixinMethodDesc);
               continue;
            }
            MethodNode targetMethod = findMethod(target, point.targetMethodName, point.targetMethodDesc);
            if (targetMethod == null) {
               log.log("[mixin] " + targetName + ": target method not found "
                     + point.targetMethodName + point.targetMethodDesc + " (skipping " + point.kind + ")");
               continue;
            }
            switch (point.kind) {
               case OVERWRITE     -> applyOverwrite(target, targetMethod, handler, selfToTarget, mixin, log);
               case INJECT_HEAD   -> applyInject(targetMethod, handler, point, selfToTarget, mixin, true, log);
               case INJECT_RETURN -> applyInject(targetMethod, handler, point, selfToTarget, mixin, false, log);
            }
         }
      }

      LoaderAwareClassWriter writer = new LoaderAwareClassWriter(reader);
      target.accept(writer);
      return writer.toByteArray();
   }

   private static void applyOverwrite(ClassNode target, MethodNode targetMethod, MethodNode handler,
                                      Remapper remapper, MixinInfo mixin, Logger log) {
      MethodNode copy = new MethodNode(API, targetMethod.access, targetMethod.name, targetMethod.desc,
            targetMethod.signature, targetMethod.exceptions.toArray(new String[0]));
      handler.accept(new MethodRemapper(copy, remapper));
      targetMethod.instructions = copy.instructions;
      targetMethod.tryCatchBlocks = copy.tryCatchBlocks;
      targetMethod.localVariables = copy.localVariables;
      targetMethod.maxStack = copy.maxStack;
      targetMethod.maxLocals = copy.maxLocals;
      log.log("[mixin] @Overwrite " + targetMethod.name + targetMethod.desc + " in " + target.name + " from " + mixin.targetClassName);
   }

   private static void applyInject(MethodNode targetMethod, MethodNode handler, InjectionPoint point,
                                   Remapper remapper, MixinInfo mixin, boolean head, Logger log) {
      Type[] handlerArgs = Type.getArgumentTypes(handler.desc);
      boolean isStatic = (handler.access & Opcodes.ACC_STATIC) != 0;
      int paramSlots = paramSlots(isStatic, handlerArgs);
      int localBase = Math.max(targetMethod.maxLocals, paramSlots);

      int returnSlot = -1;
      if (!head && point.captureReturn) {
         returnSlot = localBase;
         Type ret = Type.getReturnType(targetMethod.desc);
         localBase += ret.getSize();
      }

      InsnList body = remapBody(handler, remapper, paramSlots, localBase, returnSlot);
      int bodyLen = insnCount(body);
      InsnList cancelCheck = point.cancellable ? buildHeadCancelCheck(targetMethod.desc) : null;

      if (head) {
         targetMethod.instructions.insert(body);
         if (cancelCheck != null && bodyLen > 0) {
            AbstractInsnNode after = targetMethod.instructions.getFirst();
            for (int i = 1; i < bodyLen && after != null; i++) {
               after = after.getNext();
            }
            if (after != null) {
               targetMethod.instructions.insert(after, cancelCheck);
            } else {
               targetMethod.instructions.insert(cancelCheck);
            }
            log.log("[mixin] @Inject(HEAD,cancellable) cancel-check into " + targetMethod.name + targetMethod.desc);
         }
      } else {
         insertBeforeReturns(targetMethod.instructions, body, point, targetMethod.desc, returnSlot, cancelCheck);
         if (cancelCheck != null) {
            log.log("[mixin] @Inject(RETURN,cancellable) cancel-check into " + targetMethod.name + targetMethod.desc);
         }
      }

      int extraLocals = Math.max(0, handler.maxLocals - paramSlots);
      targetMethod.maxLocals = Math.max(targetMethod.maxLocals, localBase + extraLocals);
      log.log("[mixin] @Inject(" + (head ? "HEAD" : "RETURN") + ") " + mixin.targetClassName
            + "." + point.mixinMethodName + " into " + targetMethod.name + targetMethod.desc);
   }

   private static int paramSlots(boolean isStatic, Type[] args) {
      int slots = isStatic ? 0 : 1;
      for (Type arg : args) {
         slots += arg.getSize();
      }
      return slots;
   }

   private static int insnCount(InsnList list) {
      int n = 0;
      for (AbstractInsnNode insn = list.getFirst(); insn != null; insn = insn.getNext()) {
         n++;
      }
      return n;
   }

   /** Cancel short-circuit for HEAD injects (return value already off the stack). */
   private static InsnList buildHeadCancelCheck(String targetDesc) {
      InsnList out = new InsnList();
      LabelNode skip = new LabelNode();
      out.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CANCEL, "has", "()Z", false));
      out.add(new JumpInsnNode(Opcodes.IFEQ, skip));
      out.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CANCEL, "consume", "()Ljava/lang/Object;", false));
      emitConsumeAndReturn(out, Type.getReturnType(targetDesc), skip);
      return out;
   }

   /** Cancel short-circuit for RETURN injects (a pending return value is on the stack, pop it). */
   private static InsnList buildReturnCancelCheck(String targetDesc) {
      InsnList out = new InsnList();
      LabelNode skip = new LabelNode();
      out.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CANCEL, "has", "()Z", false));
      out.add(new JumpInsnNode(Opcodes.IFEQ, skip));
      Type ret = Type.getReturnType(targetDesc);
      out.add(new InsnNode(ret.getSize() == 2 ? Opcodes.POP2 : Opcodes.POP));
      out.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CANCEL, "consume", "()Ljava/lang/Object;", false));
      emitConsumeAndReturn(out, ret, skip);
      return out;
   }

   /** Coerce the consumed Object to the target return type and return it (or void), ending at skip. */
   private static void emitConsumeAndReturn(InsnList out, Type ret, LabelNode skip) {
      switch (ret.getSort()) {
         case Type.VOID -> {
            out.add(new InsnNode(Opcodes.POP));
            out.add(new InsnNode(Opcodes.RETURN));
         }
         case Type.BOOLEAN -> {
            out.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Boolean"));
            out.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false));
            out.add(new InsnNode(Opcodes.IRETURN));
         }
         case Type.CHAR, Type.BYTE, Type.SHORT, Type.INT -> {
            out.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Number"));
            out.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Number", "intValue", "()I", false));
            out.add(new InsnNode(Opcodes.IRETURN));
         }
         case Type.FLOAT -> {
            out.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Number"));
            out.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Number", "floatValue", "()F", false));
            out.add(new InsnNode(Opcodes.FRETURN));
         }
         case Type.LONG -> {
            out.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Number"));
            out.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Number", "longValue", "()J", false));
            out.add(new InsnNode(Opcodes.LRETURN));
         }
         case Type.DOUBLE -> {
            out.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Number"));
            out.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Number", "doubleValue", "()D", false));
            out.add(new InsnNode(Opcodes.DRETURN));
         }
         default -> {
            // object: honor Cancel null / void sentinels, else checkcast + areturn
            LabelNode notNull = new LabelNode();
            out.add(new InsnNode(Opcodes.DUP));
            out.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CANCEL, "isNullValue", "(Ljava/lang/Object;)Z", false));
            out.add(new JumpInsnNode(Opcodes.IFEQ, notNull));
            out.add(new InsnNode(Opcodes.POP));
            out.add(new InsnNode(Opcodes.ACONST_NULL));
            out.add(new InsnNode(Opcodes.ARETURN));
            out.add(notNull);
            LabelNode notVoid = new LabelNode();
            out.add(new InsnNode(Opcodes.DUP));
            out.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CANCEL, "isVoid", "(Ljava/lang/Object;)Z", false));
            out.add(new JumpInsnNode(Opcodes.IFEQ, notVoid));
            out.add(new InsnNode(Opcodes.POP));
            out.add(new InsnNode(Opcodes.ACONST_NULL));
            out.add(new InsnNode(Opcodes.ARETURN));
            out.add(notVoid);
            out.add(new TypeInsnNode(Opcodes.CHECKCAST, ret.getInternalName()));
            out.add(new InsnNode(Opcodes.ARETURN));
         }
      }
      out.add(skip);
   }

   /** Clone the handler body, rebasing local slots and rewriting returns to GOTO end. */
   private static InsnList remapBody(MethodNode handler, Remapper remapper, int paramCount, int localBase, int returnSlot) {
      InsnList out = new InsnList();
      HashMap<LabelNode, LabelNode> labels = new HashMap<>();
      for (AbstractInsnNode insn : handler.instructions) {
         if (insn instanceof LabelNode ln) {
            labels.put(ln, new LabelNode());
         }
      }
      LabelNode end = new LabelNode();
      labels.put(end, end);

      for (AbstractInsnNode insn : handler.instructions) {
         if (isReturn(insn)) {
            out.add(new JumpInsnNode(Opcodes.GOTO, end));
         } else {
            int type = insn.getType();
            if (type != AbstractInsnNode.FRAME && type != AbstractInsnNode.LINE) {
               AbstractInsnNode mapped = remapInsn(insn, remapper, paramCount, localBase, returnSlot, labels);
               if (mapped != null) {
                  out.add(mapped);
               }
            }
         }
      }
      out.add(end);
      return out;
   }

   private static boolean isReturn(AbstractInsnNode insn) {
      int op = insn.getOpcode();
      return op == Opcodes.RETURN || op == Opcodes.IRETURN || op == Opcodes.LRETURN
          || op == Opcodes.FRETURN || op == Opcodes.DRETURN || op == Opcodes.ARETURN;
   }

   /** Weave the handler body (and optional cancel-check) in front of each return in the target. */
   private static void insertBeforeReturns(InsnList targetInsns, InsnList body, InjectionPoint point,
                                           String targetDesc, int returnSlot, InsnList cancelCheck) {
      Type ret = Type.getReturnType(targetDesc);
      for (AbstractInsnNode insn : targetInsns.toArray()) {
         int op = insn.getOpcode();
         if (op == Opcodes.IRETURN || op == Opcodes.LRETURN || op == Opcodes.FRETURN
               || op == Opcodes.DRETURN || op == Opcodes.ARETURN || op == Opcodes.RETURN) {
            InsnList prefix = new InsnList();
            if (point.captureReturn && returnSlot >= 0 && op != Opcodes.RETURN) {
               int size = ret.getSize();
               prefix.add(new InsnNode(size == 2 ? Opcodes.DUP2 : Opcodes.DUP));
               prefix.add(new VarInsnNode(storeOpcode(ret), returnSlot));
            }
            prefix.add(cloneList(body));
            if (cancelCheck != null) {
               if (op != Opcodes.RETURN) {
                  prefix.add(cloneList(buildReturnCancelCheck(targetDesc)));
               } else {
                  prefix.add(cloneList(cancelCheck));
               }
            }
            targetInsns.insertBefore(insn, prefix);
         }
      }
   }

   private static InsnList cloneList(InsnList src) {
      HashMap<LabelNode, LabelNode> labels = new HashMap<>();
      for (AbstractInsnNode insn : src) {
         if (insn instanceof LabelNode ln) {
            labels.put(ln, new LabelNode());
         }
      }
      InsnList out = new InsnList();
      for (AbstractInsnNode insn : src) {
         out.add(insn.clone(labels));
      }
      return out;
   }

   private static AbstractInsnNode remapInsn(AbstractInsnNode insn, Remapper remapper,
                                             int paramCount, int localBase, int returnSlot,
                                             Map<LabelNode, LabelNode> labels) {
      if (insn instanceof VarInsnNode v) {
         return new VarInsnNode(v.getOpcode(), remapLocal(v.var, paramCount, localBase, returnSlot));
      } else if (insn instanceof MethodInsnNode m) {
         return new MethodInsnNode(m.getOpcode(), remapper.mapType(m.owner), m.name, remapper.mapMethodDesc(m.desc), m.itf);
      } else if (insn instanceof FieldInsnNode f) {
         return new FieldInsnNode(f.getOpcode(), remapper.mapType(f.owner), f.name, remapper.mapDesc(f.desc));
      } else if (insn instanceof TypeInsnNode t) {
         return new TypeInsnNode(t.getOpcode(), remapper.mapType(t.desc));
      } else if (insn instanceof LdcInsnNode ldc) {
         Object cst = ldc.cst;
         if (cst instanceof Type type) {
            cst = Type.getType(remapper.mapDesc(type.getDescriptor()));
         }
         return new LdcInsnNode(cst);
      } else if (insn instanceof InvokeDynamicInsnNode indy) {
         return new InvokeDynamicInsnNode(indy.name, remapper.mapMethodDesc(indy.desc), indy.bsm, remapBsmArgs(indy.bsmArgs, remapper));
      } else {
         return insn.clone(labels);
      }
   }

   private static Object[] remapBsmArgs(Object[] args, Remapper remapper) {
      Object[] out = new Object[args.length];
      for (int i = 0; i < args.length; i++) {
         Object arg = args[i];
         out[i] = arg instanceof Type type ? Type.getType(remapper.mapDesc(type.getDescriptor())) : arg;
      }
      return out;
   }

   /** Map a handler local slot into the target frame: params -> target's incoming slots,
    *  the capture-return slot -> its reserved slot, everything else shifted above localBase. */
   private static int remapLocal(int slot, int paramCount, int localBase, int returnSlot) {
      if (slot < paramCount) {
         return returnSlot >= 0 && slot == paramCount - 1 ? returnSlot : slot;
      }
      return localBase + (slot - paramCount);
   }

   private static int storeOpcode(Type type) {
      return switch (type.getSort()) {
         case Type.BOOLEAN, Type.CHAR, Type.BYTE, Type.SHORT, Type.INT -> Opcodes.ISTORE;
         case Type.FLOAT -> Opcodes.FSTORE;
         case Type.LONG -> Opcodes.LSTORE;
         case Type.DOUBLE -> Opcodes.DSTORE;
         default -> Opcodes.ASTORE;
      };
   }

   private static MethodNode findMethod(ClassNode owner, String name, String desc) {
      for (MethodNode m : owner.methods) {
         if (m.name.equals(name) && (desc == null || desc.isEmpty() || m.desc.equals(desc))) {
            return m;
         }
      }
      return null;
   }

   /* ClassWriter that resolves common supertypes via the game classloader (needed for
    * COMPUTE_FRAMES on Minecraft types that aren't on the agent's own classpath). */
   private static final class LoaderAwareClassWriter extends ClassWriter {
      LoaderAwareClassWriter(ClassReader reader) {
         super(reader, COMPUTE_MAXS | COMPUTE_FRAMES);
      }

      @Override
      protected String getCommonSuperClass(String type1, String type2) {
         ClassLoader loader = LOADER.get();
         if (loader == null) loader = Thread.currentThread().getContextClassLoader();
         if (loader == null) loader = this.getClass().getClassLoader();
         if (loader == null) loader = ClassLoader.getSystemClassLoader();
         try {
            Class<?> c1 = Class.forName(type1.replace('/', '.'), false, loader);
            Class<?> c2 = Class.forName(type2.replace('/', '.'), false, loader);
            if (c1.isAssignableFrom(c2)) return type1;
            if (c2.isAssignableFrom(c1)) return type2;
            if (c1.isInterface() || c2.isInterface()) return "java/lang/Object";
            do {
               c1 = c1.getSuperclass();
               if (c1 == null) return "java/lang/Object";
            } while (!c1.isAssignableFrom(c2));
            return c1.getName().replace('.', '/');
         } catch (Throwable t) {
            return "java/lang/Object";
         }
      }
   }
}
