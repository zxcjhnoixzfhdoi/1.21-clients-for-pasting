package com.mixininject.agent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/* Java agent (premain/agentmain) that loads mixin mod jars, installs a retransforming
 * ClassFileTransformer backed by MixinRegistry, forces the game classloader to see the
 * mod + agent API, and runs each jar's bootstrap entrypoint. Supports live unload/reapply. */
public final class MixinAgent {
   private static final String PREFIX = "[MixinInject] ";
   private static final String BOOTSTRAP_ENTRY = "META-INF/mixininject.bootstrap";
   private static volatile MixinRegistry registry;
   private static volatile Instrumentation instrumentation;
   private static volatile ClassFileTransformer transformer;
   private static volatile boolean active;
   private static volatile PrintWriter logWriter;

   public static void premain(String args, Instrumentation inst) {
      bootstrap(args, inst, "premain");
   }

   public static void agentmain(String args, Instrumentation inst) {
      bootstrap(args, inst, "agentmain");
   }

   /** AgentHooks.unload — remove transformer + restore original bytecode. */
   public static synchronized boolean unload() {
      Instrumentation inst = instrumentation;
      ClassFileTransformer tf = transformer;
      MixinRegistry reg = registry;
      if (active && inst != null && tf != null && reg != null) {
         try {
            inst.removeTransformer(tf);
            active = false;
            log("Transformer removed; restoring original bytecode…");
            retransformTargets(inst, reg);
            log("=== MixinAgent unloaded (bytecode restored) ===");
            return true;
         } catch (Throwable t) {
            log("unload FAILED: " + t);
            t.printStackTrace();
            return false;
         }
      }
      log("unload: nothing active");
      return false;
   }

   /** AgentHooks.reapply — reinstall transformer + retransform targets. */
   public static synchronized boolean reapply() {
      Instrumentation inst = instrumentation;
      ClassFileTransformer tf = transformer;
      MixinRegistry reg = registry;
      if (!active && inst != null && tf != null && reg != null) {
         try {
            inst.addTransformer(tf, true);
            active = true;
            log("Transformer re-installed; re-applying mixins…");
            retransformTargets(inst, reg);
            log("=== MixinAgent reapplied ===");
            return true;
         } catch (Throwable t) {
            active = false;
            log("reapply FAILED: " + t);
            t.printStackTrace();
            return false;
         }
      }
      log("reapply: skipped (active=" + active + ")");
      return active;
   }

   /** AgentHooks.isActive. */
   public static boolean isActive() {
      return active;
   }

   private static void bootstrap(String args, Instrumentation inst, String phase) {
      openLog(args);
      log("=== MixinAgent " + phase + " starting ===");
      log("Java " + System.getProperty("java.version") + "  args=" + args);
      List<File> jars = parseJars(args);
      if (jars.isEmpty()) {
         log("No mod jars found. Pass a directory or jar list as the agent argument.");
      }

      final MixinRegistry reg = new MixinRegistry(MixinAgent::log);
      registry = reg;
      instrumentation = inst;

      for (File jar : jars) {
         scanJar(reg, jar);
      }

      reg.freeze();
      log("Loaded " + reg.allMixins().size() + " mixin(s) targeting " + reg.targetClasses().size() + " class(es).");
      final ClassLoader gameLoader = installOnGameLoader(inst, reg, jars);
      exposeApi(gameLoader);
      bindHooks(gameLoader);

      ClassFileTransformer tf = new ClassFileTransformer() {
         @Override
         public byte[] transform(ClassLoader loader, String internalName, Class<?> classBeingRedefined,
                                 ProtectionDomain domain, byte[] classBytes) {
            if (MixinAgent.active && internalName != null && classBytes != null) {
               ClassLoader cl = loader != null ? loader : gameLoader;
               return reg.transform(internalName, classBytes, cl);
            }
            return null;
         }
      };
      transformer = tf;
      active = true;
      inst.addTransformer(tf, true);
      log("Transformer installed.");
      retransformTargets(inst, reg);
      runBootstraps(jars, gameLoader);
      log("=== MixinAgent " + phase + " ready ===");
   }

   /** Add the agent jar to the game CL so mixin handlers can call com.mixininject.api.Cancel. */
   private static void exposeApi(ClassLoader gameLoader) {
      if (gameLoader == null) {
         return;
      }
      try {
         CodeSource src = MixinAgent.class.getProtectionDomain().getCodeSource();
         if (src == null || src.getLocation() == null) {
            log("Cannot locate agent jar for Cancel API exposure.");
            return;
         }
         File agentJar = new File(src.getLocation().toURI());
         if (!agentJar.isFile()) {
            log("Agent location is not a jar file: " + agentJar);
            return;
         }
         installJar(gameLoader, agentJar);
         log("Exposed agent API on game CL: " + agentJar.getName());
      } catch (Throwable t) {
         log("Failed to expose agent API to game CL: " + t);
      }
   }

   /** Reflectively wire AgentHooks (in the game CL's copy) to our unload/reapply/isActive. */
   private static void bindHooks(ClassLoader gameLoader) {
      ClassLoader cl = gameLoader != null ? gameLoader : ClassLoader.getSystemClassLoader();
      try {
         Class<?> hooks = Class.forName("com.mixininject.api.AgentHooks", true, cl);
         Method bind = hooks.getMethod("bind", BooleanSupplier.class, BooleanSupplier.class, BooleanSupplier.class);
         bind.invoke(null, (BooleanSupplier) MixinAgent::unload, (BooleanSupplier) MixinAgent::reapply, (BooleanSupplier) MixinAgent::isActive);
         log("Bound AgentHooks on " + cl.getClass().getName());
      } catch (Throwable t) {
         log("Failed to bind AgentHooks: " + t);
      }
   }

   private static ClassLoader installOnGameLoader(Instrumentation inst, MixinRegistry reg, List<File> jars) {
      ClassLoader gameLoader = findGameLoader(inst, reg);
      if (gameLoader == null) {
         log("No game ClassLoader found yet; appending mod jars to system classloader search.");
         for (File jar : jars) {
            try {
               inst.appendToSystemClassLoaderSearch(new JarFile(jar));
               log("appendToSystemClassLoaderSearch: " + jar.getName());
            } catch (Throwable t) {
               log("Failed to append " + jar + ": " + t);
            }
         }
         return ClassLoader.getSystemClassLoader();
      }
      log("Game ClassLoader: " + gameLoader.getClass().getName());
      for (File jar : jars) {
         try {
            installJar(gameLoader, jar);
            log("Installed on game CL: " + jar.getAbsolutePath());
         } catch (Throwable t) {
            log("Failed to install " + jar + " on game CL: " + t);
            if (gameLoader.getClass().getName().contains("KnotClassLoader")) {
               log("Skipping system-CL fallback (Knot isolation would reject it).");
            } else {
               try {
                  inst.appendToSystemClassLoaderSearch(new JarFile(jar));
                  log("Fell back to system CL for " + jar.getName());
               } catch (Throwable t2) {
                  log("System CL fallback failed: " + t2);
               }
            }
         }
      }
      return gameLoader;
   }

   /** Find the classloader that owns Minecraft (or any Knot loader) among loaded classes. */
   private static ClassLoader findGameLoader(Instrumentation inst, MixinRegistry reg) {
      HashSet<String> wanted = new HashSet<>(reg.targetClasses());
      wanted.add("net.minecraft.client.Minecraft");
      wanted.add("net.minecraft.class_310");

      for (Class<?> c : inst.getAllLoadedClasses()) {
         if (c != null) {
            String name = c.getName();
            if (wanted.contains(name) || name.startsWith("net.minecraft.client.Minecraft")) {
               ClassLoader cl = c.getClassLoader();
               if (cl != null) {
                  return cl;
               }
            }
         }
      }
      for (Class<?> c : inst.getAllLoadedClasses()) {
         if (c != null) {
            ClassLoader cl = c.getClassLoader();
            if (cl != null && cl.getClass().getName().contains("KnotClassLoader")) {
               return cl;
            }
         }
      }
      return null;
   }

   /** Add a jar to a classloader's search path, handling Fabric's KnotClassLoader specially. */
   private static void installJar(ClassLoader loader, File jar) throws Exception {
      Path path = jar.getAbsoluteFile().toPath().normalize();
      if (loader.getClass().getName().contains("KnotClassLoader")) {
         Object delegate = null;
         try {
            Method getDelegate = loader.getClass().getDeclaredMethod("getDelegate");
            getDelegate.setAccessible(true);
            delegate = getDelegate.invoke(loader);
         } catch (NoSuchMethodException e) {
            for (Field field : loader.getClass().getDeclaredFields()) {
               if (field.getType().getName().contains("KnotClassDelegate")) {
                  field.setAccessible(true);
                  delegate = field.get(loader);
                  break;
               }
            }
         }
         if (delegate != null) {
            Method addCodeSource = delegate.getClass().getMethod("addCodeSource", Path.class);
            addCodeSource.setAccessible(true);
            addCodeSource.invoke(delegate, path);
            log("Knot addCodeSource: " + path);
         } else {
            Method addUrlFwd = loader.getClass().getMethod("addUrlFwd", URL.class);
            addUrlFwd.setAccessible(true);
            addUrlFwd.invoke(loader, path.toUri().toURL());
            log("Knot addUrlFwd (no delegate): " + path);
         }
      } else {
         URL url = path.toUri().toURL();
         for (Class<?> c = loader.getClass(); c != null; c = c.getSuperclass()) {
            try {
               Method addURL = c.getDeclaredMethod("addURL", URL.class);
               addURL.setAccessible(true);
               addURL.invoke(loader, url);
               return;
            } catch (NoSuchMethodException e1) {
               try {
                  Method addPath = c.getDeclaredMethod("addPath", Path.class);
                  addPath.setAccessible(true);
                  addPath.invoke(loader, path);
                  return;
               } catch (NoSuchMethodException e2) {
                  // keep walking up the hierarchy
               }
            }
         }
         throw new IllegalStateException("No addURL/addUrlFwd/addCodeSource on " + loader.getClass().getName());
      }
   }

   /** Invoke each mod jar's declared bootstrap entrypoint (start() or main(String[])). */
   private static void runBootstraps(List<File> jars, ClassLoader gameLoader) {
      ClassLoader cl = gameLoader != null ? gameLoader : Thread.currentThread().getContextClassLoader();
      if (cl == null) {
         cl = ClassLoader.getSystemClassLoader();
      }
      for (File jar : jars) {
         String entry = readBootstrap(jar);
         if (entry != null && !entry.isBlank()) {
            log("Bootstrap: " + entry + " from " + jar.getName() + " via " + cl.getClass().getName());
            try {
               Class<?> boot = Class.forName(entry.trim(), true, cl);
               Method entryMethod;
               try {
                  entryMethod = boot.getMethod("start");
               } catch (NoSuchMethodException e) {
                  entryMethod = boot.getMethod("main", String[].class);
               }
               if (entryMethod.getParameterCount() == 0) {
                  entryMethod.invoke(null);
               } else {
                  entryMethod.invoke(null, (Object) new String[0]);
               }
               log("Bootstrap OK: " + entry);
            } catch (Throwable t) {
               log("Bootstrap FAILED for " + entry + ": " + t);
               t.printStackTrace();
            }
         }
      }
   }

   private static String readBootstrap(File jar) {
      try (JarFile jf = new JarFile(jar)) {
         JarEntry entry = jf.getJarEntry(BOOTSTRAP_ENTRY);
         if (entry == null) {
            return null;
         }
         try (BufferedReader reader = new BufferedReader(new InputStreamReader(jf.getInputStream(entry), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
               line = line.trim();
               if (!line.isEmpty() && !line.startsWith("#")) {
                  return line;
               }
            }
         }
      } catch (IOException e) {
         log("Could not read bootstrap from " + jar + ": " + e);
      }
      return null;
   }

   private static void scanJar(MixinRegistry reg, File jar) {
      try (JarFile jf = new JarFile(jar)) {
         Enumeration<JarEntry> entries = jf.entries();
         int count = 0;
         while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.getName().endsWith(".class")) {
               byte[] bytes;
               try (InputStream in = jf.getInputStream(entry)) {
                  bytes = in.readAllBytes();
               }
               reg.register(bytes, jar.getName() + "!" + entry.getName());
               count++;
            }
         }
         log("Scanned " + count + " class(es) in " + jar.getName());
      } catch (IOException e) {
         log("Failed to read jar " + jar + ": " + e);
      }
   }

   /** Retransform any already-loaded target classes so mixins take effect immediately. */
   private static void retransformTargets(Instrumentation inst, MixinRegistry reg) {
      HashSet<String> targets = new HashSet<>(reg.targetClasses());
      if (targets.isEmpty()) {
         return;
      }
      ArrayList<Class<?>> loaded = new ArrayList<>();
      for (Class<?> c : inst.getAllLoadedClasses()) {
         if (c != null && targets.contains(c.getName())) {
            if (inst.isModifiableClass(c)) {
               loaded.add(c);
            } else {
               log("Target " + c.getName() + " is not modifiable; skipping retransform.");
            }
         }
      }
      if (loaded.isEmpty()) {
         log("No target classes are loaded yet; mixins will apply when they load.");
         return;
      }
      int done = 0;
      ClassLoader prevCtx = Thread.currentThread().getContextClassLoader();
      try {
         for (Class<?> c : loaded) {
            try {
               ClassLoader cl = c.getClassLoader();
               if (cl != null) {
                  Thread.currentThread().setContextClassLoader(cl);
               }
               inst.retransformClasses(c);
               done++;
               log("Retransformed: " + c.getName());
            } catch (Throwable t) {
               String msg = t.getMessage();
               log("Retransform FAILED for " + c.getName() + ": " + t.getClass().getSimpleName()
                     + (msg != null && !msg.isBlank() ? " — " + msg : ""));
               t.printStackTrace();
            }
         }
      } finally {
         Thread.currentThread().setContextClassLoader(prevCtx);
      }
      log("Retransformed " + done + "/" + loaded.size() + " class(es).");
   }

   private static List<File> parseJars(String args) {
      ArrayList<File> jars = new ArrayList<>();
      if (args == null || args.isBlank()) {
         return jars;
      }
      for (String token : args.split(";")) {
         String path = token.trim();
         if (path.isEmpty() || path.startsWith("log=")) {
            continue;
         }
         File file = new File(path);
         if (!file.exists()) {
            log("Path not found, skipping: " + path);
         } else if (file.isDirectory()) {
            File[] found = file.listFiles((dir, name) -> name.endsWith(".jar"));
            if (found != null) {
               for (File f : found) {
                  jars.add(f);
               }
            }
         } else if (file.getName().endsWith(".jar")) {
            jars.add(file);
         }
      }
      return jars;
   }

   private static void openLog(String args) {
      if (args == null || args.isBlank()) {
         return;
      }
      for (String token : args.split(";")) {
         String t = token.trim();
         if (t.startsWith("log=")) {
            String path = t.substring(4);
            try {
               logWriter = new PrintWriter(new FileWriter(path, true), true);
               logWriter.println(PREFIX + "[log] writing mixin logs to " + path);
               logWriter.flush();
            } catch (IOException e) {
               System.err.println(PREFIX + "could not open log file " + path + ": " + e);
            }
            return;
         }
      }
   }

   private static void log(String message) {
      String line = PREFIX + message;
      System.out.println(line);
      PrintWriter w = logWriter;
      if (w != null) {
         w.println(line);
         w.flush();
      }
   }
}
