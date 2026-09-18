package hack.echo.client.utils.player;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Frame-accurate velocity estimator using EMA smoothing.
 * get() returns blocks/tick, getPerSecond() returns blocks/second.
 */
public final class CustomGetVelocity {
	private CustomGetVelocity() {}

	private static final Map<Integer, State> STATES = new ConcurrentHashMap<>();
	private static final double SMOOTH_TAU_SEC = 0.08;
	private static final double MIN_DT_SEC = 0.001;
	private static final double MAX_DT_SEC = 0.25;
	private static final double TELEPORT_THRESHOLD_BLOCKS = 8.0;

	private static ClientLevel lastWorldRef = null;

	private static final class State {
		Vec3 lastPos;
		long lastNs;
		Vec3 velPerSecEma;
		int samples;

		State(Vec3 pos, long nowNs) {
			this.lastPos = pos;
			this.lastNs = nowNs;
			this.velPerSecEma = Vec3.ZERO;
			this.samples = 0;
		}
	}

	public static void updateAll() {
		final Minecraft mc = Minecraft.getInstance();
		if (mc == null || mc.level == null) return;

		if (lastWorldRef != mc.level) {
			STATES.clear();
			lastWorldRef = mc.level;
		}

		for (Player p : mc.level.players()) {
			updateFor(p);
		}
	}

	public static void updateFor(Entity e) {
		Objects.requireNonNull(e, "entity");
		final long nowNs = System.nanoTime();
		final Vec3 pos = new Vec3(e.getX(), e.getY(), e.getZ());

		State s = STATES.get(e.getId());
		if (s == null) {
			s = new State(pos, nowNs);
			STATES.put(e.getId(), s);
			return;
		}

		double dt = (nowNs - s.lastNs) / 1_000_000_000.0;
		if (dt <= MIN_DT_SEC) return;
		
		if (dt > MAX_DT_SEC) {
			s.lastPos = pos;
			s.lastNs = nowNs;
			return;
		}

		Vec3 dp = pos.subtract(s.lastPos);
		if (dp.length() > TELEPORT_THRESHOLD_BLOCKS) {
			s.lastPos = pos;
			s.lastNs = nowNs;
			s.velPerSecEma = Vec3.ZERO;
			s.samples = 0;
			return;
		}

		Vec3 vPerSec = dp.scale(1.0 / dt);
		double alpha = emaAlpha(dt, SMOOTH_TAU_SEC);
		
		if (s.samples == 0) {
			s.velPerSecEma = vPerSec;
		} else {
			s.velPerSecEma = lerp(s.velPerSecEma, vPerSec, alpha);
		}
		s.samples++;

		s.lastPos = pos;
		s.lastNs = nowNs;
	}

	public static Vec3 get(Entity e) {
		return getPerSecond(e).scale(1.0 / 20.0);
	}

	public static Vec3 getPerSecond(Entity e) {
		State s = STATES.get(e.getId());
		if (s != null && s.samples > 0) {
			return s.velPerSecEma;
		}
		return e.getDeltaMovement().scale(20.0);
	}

	public static double getHorizontalSpeed(Entity e) {
		Vec3 v = get(e);
		return Math.hypot(v.x, v.z);
	}

	public static double getHorizontalSpeedPerSecond(Entity e) {
		Vec3 v = getPerSecond(e);
		return Math.hypot(v.x, v.z);
	}

	public static void reset(Entity e) {
		STATES.remove(e.getId());
	}

	public static void resetAll() {
		STATES.clear();
		lastWorldRef = null;
	}

	public static boolean hasSample(Entity e) {
		State s = STATES.get(e.getId());
		return s != null && s.samples > 0;
	}

	private static double emaAlpha(double dt, double tau) {
		if (tau <= 0) return 1.0;
		double a = 1.0 - Math.exp(-dt / tau);
		return Math.max(0.0, Math.min(1.0, a));
	}

	private static Vec3 lerp(Vec3 a, Vec3 b, double t) {
		return new Vec3(
			a.x + (b.x - a.x) * t,
			a.y + (b.y - a.y) * t,
			a.z + (b.z - a.z) * t
		);
	}
}
