package dev.ambience.features.modules.movement;

import dev.ambience.features.modules.Module;
import dev.ambience.features.modules.combat.AutoTotem;
import dev.ambience.features.settings.Setting;
import dev.ambience.hooks.AutoSprintHook;
import dev.ambience.util.SilentAim;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.PlayerInput;

public class AutoSprint extends Module {
   private static AutoSprint INSTANCE;
   private final Setting<Integer> delay;
   private int moveCounter;
   private int lastAge;

   public AutoSprint() {
      super("AutoSprint", "Sprints after a short delay when you start moving.", Module.Category.MOVEMENT);
      this.delay = this.num("Delay", 2, 0, 8).setPage("General");
      this.lastAge = -1;
      INSTANCE = this;
      AutoSprintHook.bind(AutoSprint::tick);
   }

   public static AutoSprint get() {
      return INSTANCE;
   }

   public static void tick(ClientPlayerEntity player) {
      if (player == null || player.input == null) return;
      AutoSprint inst = get();
      if (inst == null || !inst.isEnabled()) return;
      if (AutoTotem.c(0)) return;  // blocked while totem-swapping

      PlayerInput input = player.input.playerInput;
      boolean isMoving = isMoving(input);
      boolean canSprint = canSprint(player);

      int age = player.age;
      if (age != inst.lastAge) {
         inst.lastAge = age;
         if (isMoving) {
            inst.moveCounter++;
         } else {
            inst.moveCounter = 0;
         }
      }

      if (inst.moveCounter >= inst.delay.getValue() && canSprint) {
         player.setSprinting(true);
      }
   }

   private static boolean isMoving(PlayerInput input) {
      return input.forward() || input.backward() || input.left() || input.right();
   }

   private static boolean canSprint(ClientPlayerEntity player) {
      if (player.isSpectator()) return false;
      if (SilentAim.a()) return false;
      HungerManager hunger = player.getHungerManager();
      if (hunger.getFoodLevel() <= 6 && !player.isCreative()) return false;
      RegistryEntry<net.minecraft.entity.effect.StatusEffect> blindness = (RegistryEntry<net.minecraft.entity.effect.StatusEffect>)(Object)StatusEffects.BLINDNESS;
      return !player.hasStatusEffect(blindness);
   }
}
