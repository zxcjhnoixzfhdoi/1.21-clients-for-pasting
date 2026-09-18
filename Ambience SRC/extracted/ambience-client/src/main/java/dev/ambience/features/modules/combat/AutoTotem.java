package dev.ambience.features.modules.combat;

import dev.ambience.features.modules.Module;
import dev.ambience.features.settings.BezierCurve;
import dev.ambience.features.settings.Setting;
import dev.ambience.hooks.AutoTotemHook;
import dev.ambience.inject.Access;
import dev.ambience.util.InventoryUtil;
import dev.ambience.util.RotationManager;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

public class AutoTotem extends Module {
   private static AutoTotem INSTANCE;
   private static long        lastPopMs;
   private static boolean     swapping;
   private static final PlayerInput ZERO_INPUT = new PlayerInput(false,false,false,false,false,false,false);

   private final Setting<Float>       health;
   private final Setting<Boolean>     silentInventory;
   private final Setting<BezierCurve> totemSpeed;
   private final Setting<BezierCurve> invOpen;
   private final Setting<Vector2f>    actionMs;

   private State  state;
   private int    totemInvSlot;   // inventory slot of totem to swap (9-35)
   private long   stateUntil;
   private boolean inventoryOpen;
   private boolean inputFrozen;
   private boolean openedScreen;
   private int    lastSwapSlot;

   public AutoTotem() {
      super("AutoTotem", "Fast F-swap totem to offhand through inventory; also refills mainhand after a pop.",
            Module.Category.COMBAT);
      this.health         = this.num("Health",         0f,  0f, 20f).setPage("General");
      this.silentInventory = this.bool("SilentInventory", false).setPage("General");
      this.totemSpeed     = this.curve("TotemSpeed",   0.78f,0f,0.94f,0.08f,35f,120f).setPage("General");
      this.invOpen        = this.curve("InvOpen",      0.78f,0f,0.94f,0.08f,55f,165f).setPage("General");
      this.actionMs       = this.vec2f("ActionMs",     450f,750f).setVec2TrackBounds(0f,2000f).setPage("General");
      this.state          = State.IDLE;
      this.totemInvSlot   = -1;
      this.lastSwapSlot   = -1;
      INSTANCE = this;

      AutoTotemHook.bind(new AutoTotemHook.Impl() {
         @Override public boolean blocksInteraction()   { return c(0); }
         @Override public boolean blocksInventoryInput() { return d(0); }
         @Override public void    freezeInput()          { e(0); }
      });
   }

   public static AutoTotem get() { return INSTANCE; }

   // c = swapping totem (blocks attacks/interactions)
   public static boolean c(int unused) { return INSTANCE != null && INSTANCE.isEnabled() && swapping; }
   // d = blocks inventory input (F-key / screen input while we're operating)
   public static boolean d(int unused) { return INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.inputFrozen; }

   public static void e(int unused) {
      if (INSTANCE == null || !c(0)) return;
      ClientPlayerEntity player = MinecraftClient.getInstance().player;
      if (player == null || player.input == null) return;
      player.input.playerInput = ZERO_INPUT;
      Access.setMoveVector(player.input, Vec2f.ZERO);
   }

   @Override public void onDisable() { resetState(); }

   @Override
   public void onPreTick() {
      if (!nullCheck() || mc.interactionManager == null) { resetState(); return; }
      ClientPlayerEntity player = mc.player;

      // check if totem pop just happened (offhand empty)
      boolean needsTotem = !player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)
                        && player.getHealth() <= this.health.getValue() + 1f;
      if (!needsTotem && this.state == State.IDLE) return;
      if (!needsTotem && this.state != State.IDLE) { resetState(); return; }

      switch (this.state) {
         case IDLE -> beginSwap();
         case OPEN_INV -> {
            if (System.currentTimeMillis() >= this.stateUntil) openInventory();
         }
         case MOVE_TO_OFFHAND -> {
            if (System.currentTimeMillis() >= this.stateUntil) doFSwap();
         }
         case CLOSE_INV -> {
            if (System.currentTimeMillis() >= this.stateUntil) closeInventory();
         }
      }
   }

   private void beginSwap() {
      this.totemInvSlot = findTotem();
      if (this.totemInvSlot == -1) return;
      swapping = true;
      this.inputFrozen = true;
      this.stateUntil = System.currentTimeMillis() + curveDelay(this.invOpen.getValue());
      this.state = State.OPEN_INV;
   }

   private void openInventory() {
      if (this.silentInventory.getValue()) {
         this.openedScreen = false;
      } else {
         mc.setScreen(new InventoryScreen(mc.player));
         this.openedScreen = true;
      }
      this.stateUntil = System.currentTimeMillis() + curveDelay(this.totemSpeed.getValue());
      this.state = State.MOVE_TO_OFFHAND;
   }

   private void doFSwap() {
      // click totem onto offhand: click source slot, then shift-click to offhand
      PlayerScreenHandler handler = mc.player.playerScreenHandler;
      int netSlot = totemInventoryToScreenSlot(this.totemInvSlot);
      mc.interactionManager.clickSlot(handler.syncId, netSlot, 0, SlotActionType.PICKUP, mc.player);
      mc.interactionManager.clickSlot(handler.syncId, 45, 0, SlotActionType.PICKUP, mc.player); // 45 = offhand
      lastPopMs = System.currentTimeMillis();
      this.stateUntil = System.currentTimeMillis() + curveDelay(this.totemSpeed.getValue());
      this.state = State.CLOSE_INV;
   }

   private void closeInventory() {
      if (this.openedScreen && mc.currentScreen instanceof InventoryScreen) {
         mc.setScreen(null);
      }
      resetState();
   }

   private void resetState() {
      swapping = false;
      this.inputFrozen = false;
      this.state = State.IDLE;
      this.totemInvSlot = -1;
   }

   private int findTotem() {
      // prefer inventory slots first (avoid hotbar waste)
      PlayerInventory inv = mc.player.getInventory();
      for (int i = 9; i < inv.size(); i++) {
         if (inv.getStack(i).isOf(Items.TOTEM_OF_UNDYING)) return i;
      }
      for (int i = 0; i < 9; i++) {
         if (inv.getStack(i).isOf(Items.TOTEM_OF_UNDYING)) return i;
      }
      return -1;
   }

   /** Convert player inventory slot (0-35) to screen handler slot id. */
   private static int totemInventoryToScreenSlot(int invSlot) {
      // PlayerScreenHandler: 9-35 = inventory rows 1-3+hotbar mapped to slots 9-44
      return invSlot < 9 ? invSlot + 36 : invSlot;
   }

   private long curveDelay(BezierCurve curve) {
      Vector2f v = this.actionMs.getValue();
      int lo = Math.round(v.x()), hi = Math.round(v.y());
      long base = lo >= hi ? lo : lo + ThreadLocalRandom.current().nextInt(hi - lo + 1);
      return base;
   }

   private enum State { IDLE, OPEN_INV, MOVE_TO_OFFHAND, CLOSE_INV }
}
