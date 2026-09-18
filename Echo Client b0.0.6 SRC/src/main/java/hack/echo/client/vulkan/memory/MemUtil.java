package hack.echo.client.vulkan.memory;

import com.azure.json.implementation.jackson.core.util.ByteArrayBuilder;
import io.netty.buffer.ByteBuf;
import lombok.experimental.UtilityClass;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

@UtilityClass
public class MemUtil {


    public void putMat4(long p, Matrix4f mat) {
        MemoryUtil.memPutFloat(p,      mat.m00()); MemoryUtil.memPutFloat(p +  4, mat.m01());
        MemoryUtil.memPutFloat(p +  8, mat.m02()); MemoryUtil.memPutFloat(p + 12, mat.m03());
        MemoryUtil.memPutFloat(p + 16, mat.m10()); MemoryUtil.memPutFloat(p + 20, mat.m11());
        MemoryUtil.memPutFloat(p + 24, mat.m12()); MemoryUtil.memPutFloat(p + 28, mat.m13());
        MemoryUtil.memPutFloat(p + 32, mat.m20()); MemoryUtil.memPutFloat(p + 36, mat.m21());
        MemoryUtil.memPutFloat(p + 40, mat.m22()); MemoryUtil.memPutFloat(p + 44, mat.m23());
        MemoryUtil.memPutFloat(p + 48, mat.m30()); MemoryUtil.memPutFloat(p + 52, mat.m31());
        MemoryUtil.memPutFloat(p + 56, mat.m32()); MemoryUtil.memPutFloat(p + 60, mat.m33());
    }

    public void putVec3(long p, float x, float y, float z) {
        MemoryUtil.memPutFloat(p, x);
        MemoryUtil.memPutFloat(p + 4, y);
        MemoryUtil.memPutFloat(p + 8, z);
    }

    public void putVec4(long p, float x, float y, float z, float w) {
        MemoryUtil.memPutFloat(p, x);
        MemoryUtil.memPutFloat(p + 4, y);
        MemoryUtil.memPutFloat(p + 8, z);
        MemoryUtil.memPutFloat(p + 12, w);
    }

    public void putInt(long p, int x) {
        MemoryUtil.memPutInt(p, x);
    }

    public void putIVec3(long p, int x, int y, int z) {
        MemoryUtil.memPutInt(p, x);
        MemoryUtil.memPutInt(p + 4, y);
        MemoryUtil.memPutInt(p + 8, z);
    }

    public void putIVec4(long p, int x, int y, int z, int w) {
        putIVec3(p, x, y, z);
        MemoryUtil.memPutInt(p + 12, w);
    }


    public void putFloat(long p, float x) {
        MemoryUtil.memPutFloat(p, x);
    }

    public int getInt(long p) {
        return p != 0 ? MemoryUtil.memGetInt(p) : 0;
    }

    public void memcpy(long src, long dst, long size) {
        MemoryUtil.memCopy(src, dst, size);
    }

    public void memcpy(byte[] src, long dst, int size) {
        for (int i = 0; i < size; i++) {
            MemoryUtil.memPutByte(dst + i, src[i]);
        }
    }

    public void memcpy(int[] src, long dst, int size) {
        for (int i = 0; i < size; i++) {
            MemoryUtil.memPutInt(dst + i * 4L, src[i]);
        }
    }

    public void memcpy(long[] src, long dst, int size) {
        for (int i = 0; i < size; i++) {
            MemoryUtil.memPutLong(dst + i * 8L, src[i]);
        }
    }
}
