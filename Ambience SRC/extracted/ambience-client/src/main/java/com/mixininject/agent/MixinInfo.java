package com.mixininject.agent;

import java.util.ArrayList;
import java.util.List;

/* A registered mixin class + its resolved target + the injections it carries. */
public final class MixinInfo {
   public final String mixinClassName;      // dotted
   public final String mixinInternalName;   // slashed
   public final String targetClassName;     // dotted
   public final String targetInternalName;  // slashed
   public final byte[] mixinBytes;
   public final List<InjectionPoint> injections = new ArrayList<>();

   public MixinInfo(String mixinClassName, String targetClassName, byte[] mixinBytes) {
      this.mixinClassName = mixinClassName;
      this.mixinInternalName = mixinClassName.replace('.', '/');
      this.targetClassName = targetClassName;
      this.targetInternalName = targetClassName.replace('.', '/');
      this.mixinBytes = mixinBytes;
   }
}
