# Echo Scripting API — Design Notes

A TS/JS-driven module system. Goal: let users write/vibecode niche modules in a sandbox without bloating the Java client. The Java side stays as a small, stable host; everything one-shot lives in scripts.

The current Java `Feature` model maps cleanly onto a script-shaped abstraction: declarative metadata, declarative settings, event handlers as methods, lifecycle hooks. The host engine just needs to expose the same surface.

---

## 1. Module shape

A script module mirrors `Feature` 1:1. Java has:

```java
public class TriggerBot extends Feature {
    public TriggerBot() {
        super(new FeatureInfo("Trigger Bot", "...", Category.COMBAT));
    }
    private final BoolSetting attackShielded = new BoolSetting("Attack Shields", true);

    @Override public void onEnable()  { ... }
    @Override public void onDisable() { ... }

    @EventSubscribe
    private void onInput(EventHandleInput.Early e) { ... }
}
```

Script equivalent:

```ts
import { defineModule, Category, settings, events, player, combat, input } from "echo";

export default defineModule({
  name: "Trigger Bot",
  description: "Auto attack when crosshair on target",
  category: Category.Combat,
  key: -1,                     // optional default keybind

  settings: {
    attackShielded: settings.bool("Attack Shields", true),
    cooldown:       settings.range("Cooldown", 90, 100, 0, 100, 1),
    crit:           settings.mode("Crit Mode", "pCrit", ["pCrit", "normal", "None"]),
    items:          settings.itemPicker("Item Whitelist"),
    delay:          settings.int("Reaction Delay", 0, 0, 500),
    sim:            settings.bool("Click Simulation", false),
  },

  onEnable()  { /* reset state */ },
  onDisable() { /* cleanup */ },

  events: {
    "input.early"(e, ctx) {
      if (ctx.isNull() || ctx.screen) return;
      const target = combat.getCrosshairTarget();
      if (!target || !combat.isTargetAllowed(target)) return;
      if (!ctx.settings.attackShielded.get() && target.isBlocking) return;
      input.simulateClick("attack", ctx.settings.sim.get());
    },
    "packet.receive"(e, ctx) {
      if (e.packet.type === "ClientboundPlayerPosition") ctx.state.target = null;
    },
  },
});
```

Mental model: a single object literal instead of a class. The host instantiates it, runs `initSettings` by walking the `settings` map, registers event subscriptions, and calls lifecycle hooks. `this` is avoided — handlers get a `ctx` with settings, shared state, and the module instance.

---

## 2. Categories

Mirror `Category.java` exactly. `INTERNALS` is omitted on purpose — scripts shouldn't impersonate internal modules.

```ts
export enum Category {
  Combat   = "COMBAT",
  Movement = "MOVEMENT",
  Render   = "RENDER",
  Macros   = "MACROS",
  Utility  = "UTILITY",
}
```

---

## 3. Settings API

Every Java setting type has a script factory. The factory returns a handle with `.get()` / `.set()` / `.onChange(fn)` / `.visibleWhen(fn)` (mirrors `Setting.dependency`).

| Java type                | Script factory                                                            | Value type                  |
| ------------------------ | ------------------------------------------------------------------------- | --------------------------- |
| `BoolSetting`            | `settings.bool(name, def)`                                                | `boolean`                   |
| `IntSetting`             | `settings.int(name, def, min, max, suffix?)`                              | `number`                    |
| `FloatSetting`           | `settings.float(name, def, min, max, step, suffix?)`                      | `number`                    |
| `RangeSetting`           | `settings.range(name, defMin, defMax, lo, hi, step, suffix?)`             | `{ min, max, random() }`    |
| `ModeSetting`            | `settings.mode(name, def, modes[])`                                       | `string` + `.is()/.cycle()` |
| `MultiModeSetting`       | `settings.multiMode(name, modes[])`                                       | `Record<string, boolean>`   |
| `StringSetting`          | `settings.string(name, placeholder, { multi?, max? })`                    | `string` / `string[]`       |
| `ColorSetting`           | `settings.color(name, r, g, b, a, { showAlpha? })`                        | `{ rgba(), hsb(), rgb() }`  |
| `KeybindSetting`         | `settings.keybind(name, defaultKey)`                                      | `number` (GLFW key)         |
| `HotbarSelectionSetting` | `settings.hotbar(name)`                                                   | `boolean[9]`                |
| `ItemPickerSetting`      | `settings.itemPicker(name)`                                               | `Set<Identifier>`           |
| `RegistryPickerSetting`  | `settings.registryPicker(name, "block" \| "entity" \| "effect" \| ...)`   | `Set<Identifier>`           |

Dependencies are first-class to keep parity with the existing UI:

```ts
const mode = settings.mode("Mode", "vanilla", ["vanilla", "Grim"]);
const ticks = settings.int("Delay Ticks", 2, 0, 10).visibleWhen(() => mode.is("Grim"));
```

Settings auto-persist (Java currently writes to `_autosave` profile via `Setting.notifyChange()`). Same applies here — the engine triggers save on change.

---

## 4. Event API

The Java `EventManager` dispatches by class. The script bridge maps event classes → string keys. Each event has the same getters/setters available on the script side.

### Event keys

Lifecycle / tick:
- `tick` — `EventTick`
- `playerTick` — `EventClientPlayerTick`
- `systemUpdate` — `EventSystemUpdate`
- `levelChange` — `EventLevelChange`
- `setScreen` — `EventSetScreen`

Movement / input:
- `move.pre` / `move.post` — `EventMove.Pre/Post` (mutable x/y/z/yaw/pitch/onGround)
- `movePos` — `EventMovePos`
- `movementInput` — `EventMovementInput`
- `input` — `EventInput`
- `input.early` / `input.pre` / `input.post` — `EventHandleInput.*`
- `key` — `EventKey` (key, action)
- `mouse.press` / `mouse.update` — `MousePressEvent`, `MouseUpdateEvent`
- `sprint` — `SprintEvent` (`setShouldSprint`)
- `slowdown` — `EventSlowdown` (`setSlowdownMultiplier`, `setSlowDueToItem`, `setMovingSlowly`, `setSpoofPassenger`)

Combat / interaction:
- `attack.start` — `EventStartAttack`
- `attack.entity.pre` / `attack.entity.post` — `EventOnAttackEntity.Pre/Post`
- `swingHand` — `EventSwingHand`
- `useItem.start.pre/post` — `EventStartUseItem`
- `itemUse` — `EventItemUse`
- `useItemOn` — `EventPerformUseItemOn`
- `tridentStop` — `EventOnTridentStoppedUsing`
- `swap` — `EventSwapHands`
- `shieldBreak` — `EventShieldBreak`

Inventory / world:
- `slotClick` — `EventOnSlotClick`
- `slotChange` — `EventInventorySlotChange`
- `hotbar` — `EventHotbarChange`
- `blockBreaking` — `EventBlockBreaking`
- `blockUpdate` — `EventBlockUpdate`
- `chunk.load` / `chunk.unload` / `chunk.occlusion` — `EventChunkLoad/Unload/Occlusion`

Network:
- `packet.send` — `EventPacketSend` (cancelable; `e.packet`)
- `packet.receive` — `EventPacketReceive`

Render:
- `render2d` — `EventRender2DGui` (gives a `Draw2D` handle, scaled width/height, matrix stack)
- `render3d` — `EventRender3D` (matrices, camera, `Draw3D`)
- `earlyBeginFrame` — `EventEarlyBeginFrame`
- Overlay events: `render.fire`, `render.nausea`, `render.portal`, `render.spyglass`, `render.vignette`, `render.scoreboard`, `render.effects`, `render.entityOutline`, `render.floatingItem`, `render.entityOutlineFramebuffer`
- `setPerspective` — `EventSetPerspective`

Audio:
- `playSound` — `EventPlaySound`

### Handler signature

```ts
events: {
  "move.pre"(e, ctx) {
    if (ctx.player.onGround) e.y = 0;        // setters proxied
    if (someCondition) e.cancel();           // Event.cancel()
  }
}
```

Priority + stage are passed via the key suffix or an options form:

```ts
events: {
  "packet.send": { priority: "HIGH", handler(e, ctx) { ... } },
}
```

Priorities mirror `EventSubscribe.Priority` (`LOWEST`, `LOW`, `NORMAL`, `HIGH`, `HIGHEST`).

---

## 5. Context object (`ctx`)

Passed to every handler/lifecycle hook. Keeps scripts away from raw `Minecraft.getInstance()` chains.

```ts
ctx.mc            // host-controlled handle (read mostly)
ctx.player        // null-safe wrapper around mc.player
ctx.world         // mc.level
ctx.screen        // mc.screen (null when in-game)
ctx.options       // mc.options (key bindings)
ctx.settings      // typed settings map for THIS module
ctx.state         // mutable scratch object (per-module persistent across handlers)
ctx.module        // self-ref (toggle, isEnabled, getName)
ctx.isNull()      // mirror of Feature.isNull(): player==null || level==null
ctx.tickCount     // mc.player.tickCount
ctx.partialTick   // current render partial tick
```

`ctx.player` proxies the common reads/writes used by features (mirrors `Imports`/`PlayerUtils`):

```ts
ctx.player.pos / .prevPos / .velocity
ctx.player.yaw / .pitch
ctx.player.health / .maxHealth / .hunger
ctx.player.isUsingItem / .isSprinting / .isSneaking / .onGround / .inWater
ctx.player.mainHand / .offHand        // ItemStack-like
ctx.player.setMotion(x?, y?, z?)
ctx.player.setSprinting(bool)
ctx.player.distanceTo(entityOrVec)
ctx.player.swing(hand?)
```

---

## 6. Host modules (the namespaces)

These are the curated bridges. Each one wraps a Java util so scripts don't import Minecraft types directly. Names track the existing util classes so the mental model stays consistent.

### `combat` — wraps `CombatUtils`, `TargetUtils`
```ts
combat.canCrit(): boolean
combat.getCrosshairTarget(): Entity | null
combat.getCurrentItemCrosshairTarget(): Entity | null
combat.isTargetAllowed(entity): boolean
combat.resolveTarget(candidate): Entity | null
combat.isInFOV(target, fov): boolean
combat.selectNearestTarget(entities): Entity | null
combat.isBot(entity): boolean
combat.lastAttacked(ticks): Entity | null
```

### `player` — wraps `PlayerUtils`
```ts
player.isMoving(): boolean
player.isShieldingOffhand(): boolean
player.isShieldingMainhand(): boolean
player.getClosestEnemy(range): Entity | null
player.getNearestEnemyPlayer(): Player | null
player.getNearestLivingEntity(range): Entity | null
player.distanceToGround(): number
player.distanceTo(target): { vertical, horizontal, total }
player.predictNextTickFallDistance(): number
player.isFriend(name): boolean
player.isCollidingWithHitbox(other?): boolean
```

### `inventory` — wraps `InventoryUtils`, `ContainerUtils`, `CursorUtil`
```ts
inventory.setSlot(slot, simulateInput?)
inventory.swapToOffhand()
inventory.silentSwapTo(slot) / silentSwapBack()
inventory.find.sword() / .axe() / .totem() / .spear() / .chestplate()
inventory.has(item | predicate): boolean
inventory.count(item | predicate, opts?: { skipHotbar }): number
inventory.holding.sword() / .axe() / .pickaxe() / .shovel() / .hoe() / .spear()
inventory.hotbar.empty(): number[]
inventory.mainHand / inventory.offHand
inventory.findWithEnchant(predicate, enchantId, allowNone?)
```

### `world` / `blocks` — wraps `WorldUtils`, `BlockUtils`
```ts
blocks.is(pos, blockId): boolean
blocks.distanceTo(pos): number
blocks.interactWith(hit, swing?)
blocks.willCollideWithEntity(pos, blockId): boolean
blocks.respawnAnchor.charged(pos) / .uncharged(pos)
blocks.getTargetPlacePos(): BlockPos | null
blocks.isValidPlacement(pos): boolean
world.getBlock(pos): { id, state }
world.getEntities(filter?): Entity[]
world.players(): Player[]
```

### `rotation` — wraps `RotationHandler`, `RotationUtils`
This is critical for combat scripts. Use the cooperative builder so multiple modules don't fight.
```ts
rotation.aim(ctx)                   // returns RotationBuilder
  .at(target | vec3)
  .speed(yawSpeed, pitchSpeed)
  .silent(true)
  .priority("HIGH")
  .commit()                         // attempts takeControl, applies rotation

rotation.releaseControl(ctx)
rotation.isControlledByMe(ctx): boolean
rotation.silent: { yaw, pitch, active }
rotation.calculate(from, to): [yaw, pitch]
rotation.predictAimPoint(entity, ticks, mode): Vec3
rotation.rayTraceTo(end, partialTick): HitResult
rotation.closestPointToBox(box, partialTick): Vec3
```

### `input` — wraps `InputHandler`
```ts
input.isKeyDown(glfwKey): boolean
input.isMouseDown(button): boolean
input.isBindDown("attack" | "use" | "jump" | KeyMappingRef): boolean
input.simulateClick("attack" | "use" | KeyMappingRef, simulate: boolean)
input.simulateKeyPress(glfwKey) / simulateKeyRelease(glfwKey)
```

### `network` — wraps `NetworkUtil`
```ts
network.send(packet)                  // packet built via factory (see below)
network.packets.swing(hand): Packet
network.packets.useItem(hand): Packet
network.packets.attack(entityId): Packet
network.packets.move(x, y, z, onGround): Packet
network.packets.lookAt(yaw, pitch, onGround): Packet
network.packets.swap(): Packet
// generic, last-resort:
network.packets.raw(typeId, builderFn): Packet
```

### `chat`
```ts
chat.send(message)
chat.print(message)         // client-side
chat.notify(title, body?, type?)   // routes to NotificationsHudModule
```

### `render2` / `render3` — exposed only inside render handlers
```ts
events: {
  "render2d"(e, ctx) {
    e.draw.rect(x, y, w, h, { radius, color });
    e.draw.text(font, str, x, y, size, color, { shadow: true });
    e.draw.image(tex, x, y, w, h, { radius, alpha });
    e.draw.scissor(x, y, w, h, () => { /* clipped draws */ });
  },
  "render3d"(e, ctx) {
    e.target.box(x, y, z, sx, sy, sz, color);
    e.target.tracer(x, y, z, color);
    e.target.line(ax, ay, az, bx, by, bz, color);
    e.target.particle(x, y, z, size, color, uv, rotation);
  },
}
```

The host picks GL vs Vulkan via `VulkanUtil.isVulkanLoaded()` — scripts never see the backend split. Color helpers: `color.rgba(r,g,b,a)`, `color.hsb(h,s,b,a?)`, `color.hex("#ff8800ee")`.

### `features` — talk to other modules
```ts
features.get("TriggerBot"): ModuleRef | null
features.isEnabled(name): boolean
features.toggle(name)
features.byCategory(Category.Combat): ModuleRef[]
features.list(): ModuleRef[]
```

Useful for "if Piercing is enabled, prefer its target" patterns seen in Java. By name (string) rather than class so scripts don't reach into Java packages.

### `time` / `timer`
```ts
const t = timer.create();
t.reset(); t.hasReached(ms);

time.now(): number              // ms
time.tick(): number             // game tick counter
time.partial(): number          // current partial tick
```

### `keys`
GLFW constants, keybind helpers:
```ts
keys.GLFW_KEY_F, keys.GLFW_MOUSE_BUTTON_LEFT, ...
keys.fromName("F") / keys.toName(code)
keys.encodeMouseButton(n)       // matches the 0x80000000 | mb encoding from InputHandler
```

### `log`
```ts
log.info(...) / log.warn(...) / log.error(...) / log.debug(...)
```

---

## 7. Lifecycle / loader

**Layout.** Place scripts in `~/.echo/scripts/<name>/index.{ts,js}` (or single `.js` file directly). On client start (and on hot-reload command), the engine:

1. Reads each script directory.
2. Bundles TS → JS (esbuild) and runs via the embedded JS runtime.
3. Calls the `defineModule` default export, builds a Java-side `Feature` proxy that forwards `onEnable`/`onDisable`/event dispatch to the script.
4. Registers the proxy with `FeatureManager` like any other module — they show up in the ClickGUI, in the arraylist, in keybinds, in profile saves.

**Hot reload.** A `/script reload <name>` command tears down the proxy (`feature.setEnabled(false)`, `EventManager.unregister`, drops settings) and re-loads. Scripts that throw during load are quarantined; the rest keep working.

**Sandboxing.**
- No `java.*` access from scripts. Only the `echo` namespace.
- No network/IO unless the script declares it in a manifest (`permissions: ["net", "fs"]`) and the user accepts. Default-deny.
- `setTimeout`/`setInterval` are routed through the tick loop so they get cleaned up on disable.

**Engine choice.** Recommended: **Rhino** (pure Java, ES6-ish via Mozilla), or **GraalJS** (faster, but big native dep). Rhino keeps the mod jar small and avoids native footgun on Vulkan/GL coexistence. Either way, expose host objects as `ScriptableObject` proxies that wrap the existing util statics — no reflection per call.

**Type definitions.** Ship `echo.d.ts` so users get autocomplete/typecheck. The file is generated from a small registry on the Java side (one source of truth for events, settings, namespaces) so it never drifts.

---

## 8. Turing-completeness notes

The runtime is a real JS engine, so loops/recursion/closures/objects are free. The only restrictions are:

- Handlers run on the MC main thread. Long loops will freeze the client. Provide `await time.nextTick()` and `time.delay(ms)` for cooperative scripts. (Implemented as continuations on the tick loop.)
- No raw threading from scripts. If you need work off-thread, expose `tasks.run(fn)` that runs on a worker pool and resolves a promise on the main thread.
- Memory is bounded per-script (configurable cap — Rhino has `Context.setMaximumInterpreterStackDepth` and instruction count callbacks for runaway loops).

---

## 9. Mapping cheat-sheet (Java → script)

| Java                                                  | Script                                                  |
| ----------------------------------------------------- | ------------------------------------------------------- |
| `extends Feature` + `@FeatureInfo`                    | `defineModule({ name, description, category, key })`    |
| Field `BoolSetting foo = new BoolSetting(...)`        | `settings: { foo: settings.bool(...) }`                 |
| `@EventSubscribe void onX(EventX e)`                  | `events: { "x"(e, ctx) {} }`                            |
| `@EventSubscribe(priority = HIGH)`                    | `{ priority: "HIGH", handler }`                         |
| `onEnable` / `onDisable`                              | `onEnable() {}` / `onDisable() {}`                      |
| `mc.player.xxx`                                       | `ctx.player.xxx`                                        |
| `Echo.featureManager.getFeatureByClass(X.class)`      | `features.get("X")`                                     |
| `EventManager.post(new Foo())`                        | (not exposed — scripts can't post host events)          |
| `RotationHandler.aim(this).at(...).commit()`          | `rotation.aim(ctx).at(...).commit()`                    |
| `InputHandler.simulateClick(mc.options.keyAttack, b)` | `input.simulateClick("attack", b)`                      |
| `NetworkUtil.send(packet)`                            | `network.send(network.packets.swing("MAIN"))`           |
| `mc.options.keyAttack` (raw KeyMapping)               | `"attack"` (named) — engine resolves                    |

---

## 10. Open questions / suggested follow-ups

1. **Packet exposure.** Full packet builders is a lot of surface. Start with the ~15 packets actually used by existing modules (move, look, swing, useItem, attack, swap, place, slotClick, container click, chat, plugin). Add more on demand.
2. **Render namespace parity.** Java renders go through `Draw2D`/`Draw3D` and a shader pipeline. Restrict scripts to the `Draw*` API (no custom shaders) initially — keeps the GL/Vulkan abstraction intact.
3. **Auth builds.** The `auth` constant in `stonecutter.gradle.kts` rewrites strings via `SettingNameTransformer`. Scripts won't go through that transformer, so `features.json` validation must allow script-defined feature names through a separate registry — otherwise auth-mode clients will refuse to load scripts.
4. **HUD modules.** `HudFeature` / `NotificationsHudModule` / `ArrayListModule` etc. — decide if scripts can declare HUD elements (separate `hud: { render(ctx, hud) {} }` block) or if they're 2D-render-event only.
5. **Storage.** Add `storage.get(key)`/`set(key, value)` per-module persisted JSON, separate from settings, for things like learned offsets / last targets.
6. **Discovery.** `/script list`, `/script enable`, `/script disable`, `/script reload` slash commands. Mirror the existing `ConfigCommandHandler` style.

---

## Worked example — port of `Sprint.java`

```ts
import { defineModule, Category, settings, player } from "echo";

export default defineModule({
  name: "Sprint",
  description: "Auto sprint",
  category: Category.Movement,
  settings: { omni: settings.bool("Omni", false) },

  onDisable(ctx) {
    if (ctx.player.exists) {
      ctx.player.setSprinting(false);
      // SprintController.reset() — exposed as:
      ctx.player.sprint.reset();
    }
  },

  events: {
    sprint(e, ctx) {
      if (ctx.player.sprint.forceStopped) { e.shouldSprint = false; return; }
      if (player.isMoving()) e.shouldSprint = true;
    },
  },

  concat(ctx) { return ctx.settings.omni.get() ? "Omni" : ""; },
});
```

That's the whole module — about half the LOC of the Java version, no imports of Minecraft internals, hot-reloadable, deletable by removing one folder.
