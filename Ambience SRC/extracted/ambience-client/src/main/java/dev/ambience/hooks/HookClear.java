package dev.ambience.hooks;

public final class HookClear {
   private HookClear() {
   }

   public static void clearAll() {
      AimAssistHook.bind(null);
      AnchorMacroHook.bind(null);
      AntiBlindHook.bind(null);
      AutoEscHook.bind(null);
      AutoSprintHook.bind(null);
      AutoTotemHook.bind(null);
      BreachSwapHook.bind(null);
      CrystalOptimizerHook.bind(null);
      CustomSkyboxHook.bind(null);
      FreecamHook.bind(null);
      FreelookHook.bind(null);
      FullbrightHook.bind(null);
      GuiRuntime.bind(null);
      JumpResetHook.bind(null);
      LowFireHook.bind(null);
      NoBlastHook.bind(null);
      NoInvisHook.bind(null);
      NoSmokeHook.bind(null);
      OmniSprintHook.bind(null);
      ProjectileAimHook.bind(null);
      ShieldStunHook.bind(null);
      TotemAnimationHook.bind(null);
      ViewModelHook.bind(null);
      WTapHook.bind(null);
      STapHook.bind(null);
      XRayHook.bind(null);
   }
}
