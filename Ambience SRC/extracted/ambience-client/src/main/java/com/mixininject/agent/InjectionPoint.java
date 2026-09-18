package com.mixininject.agent;

/* One @Inject / @Overwrite target parsed off a mixin method. */
public final class InjectionPoint {
   public final Kind kind;
   public final String mixinOwner;        // internal name of the mixin class
   public final String mixinMethodName;   // handler method on the mixin
   public final String mixinMethodDesc;
   public final String targetMethodName;  // method to inject into / overwrite
   public final String targetMethodDesc;
   public final boolean captureArgs;
   public final boolean captureReturn;
   public final boolean cancellable;

   public InjectionPoint(Kind kind, String mixinOwner, String mixinMethodName, String mixinMethodDesc,
                         String targetMethodName, String targetMethodDesc,
                         boolean captureArgs, boolean captureReturn) {
      this(kind, mixinOwner, mixinMethodName, mixinMethodDesc, targetMethodName, targetMethodDesc,
           captureArgs, captureReturn, false);
   }

   public InjectionPoint(Kind kind, String mixinOwner, String mixinMethodName, String mixinMethodDesc,
                         String targetMethodName, String targetMethodDesc,
                         boolean captureArgs, boolean captureReturn, boolean cancellable) {
      this.kind = kind;
      this.mixinOwner = mixinOwner;
      this.mixinMethodName = mixinMethodName;
      this.mixinMethodDesc = mixinMethodDesc;
      this.targetMethodName = targetMethodName;
      this.targetMethodDesc = targetMethodDesc;
      this.captureArgs = captureArgs;
      this.captureReturn = captureReturn;
      this.cancellable = cancellable;
   }

   @Override
   public String toString() {
      return this.kind + " " + this.mixinOwner + "." + this.mixinMethodName + this.mixinMethodDesc
           + " -> " + this.targetMethodName + this.targetMethodDesc;
   }

   public enum Kind {
      INJECT_HEAD,
      INJECT_RETURN,
      OVERWRITE
   }
}
