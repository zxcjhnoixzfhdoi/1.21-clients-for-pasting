package hack.echo.client.auth;

import hack.echo.client.Echo;
import net.minecraft.util.Mth;

public class MathProt {

    private static long serverInteger = 0L;
    private static long secretLocalKey = 0L;
    private static long expectedRuntimeConstant = 0L;

    public static void updateVerification(long serverInt, long secretKey, long runtimeConstant) {
        serverInteger = serverInt;
        secretLocalKey = secretKey;
        expectedRuntimeConstant = runtimeConstant;
    }

    private static long computeRuntimeConstant() {
        long xor = serverInteger ^ secretLocalKey;
        return xor + 21332L;
    }

    public static float getEnforcedPitch(float requestedPitch) {
        //? if auth {
        long computedConstant = computeRuntimeConstant();
        long delta = expectedRuntimeConstant - computedConstant;
        int authGap = (Echo.authManager != null && Echo.authManager.validateSession()) ? 0 : 1;
        long mismatch = delta | authGap;
        float candidatePitch = mismatch == 0 ? requestedPitch : -90f;
        return Mth.clamp(candidatePitch, -90f, 90f);
        //?}
        //? if !auth {
        /*return Mth.clamp(requestedPitch, -90f, 90f);
        *///?}
    }
}
