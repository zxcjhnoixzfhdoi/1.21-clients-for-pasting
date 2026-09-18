package hack.echo.client.render2.impl.opengl.utils;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL43;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.lwjgl.opengl.GL20.*;

public class GLShaderUniforms {

    private static final Map<String, Integer> LOCATION_CACHE = new ConcurrentHashMap<>();

    private static int getLocation(int programId, String name) {
        String key = programId + ":" + name;
        return LOCATION_CACHE.computeIfAbsent(key, k -> glGetUniformLocation(programId, name));
    }

    public static void uniform1i(int programId, String name, int i) {
        glUniform1i(getLocation(programId, name), i);
    }

    public static void uniformTexture(int programId, String name, int texture) {
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texture);
        uniform1i(programId,name, 0);
    }

    public static void uniform1ui(int programId, String name, int i) {
        GL43.glUniform1ui(getLocation(programId,name),i);
    }

    public static void uniform2i(int programId, String name, int i, int j) {
        glUniform2i(getLocation(programId, name), i, j);
    }

    public static void uniform1f(int programId, String name, float f) {
        glUniform1f(getLocation(programId, name), f);
    }

    public static void uniform2f( int programId, String name, float f, float g) {
        glUniform2f(getLocation(programId, name), f, g);
    }

    public static void uniform3f(int programId, String name, float f, float g, float h) {
        glUniform3f(getLocation(programId, name), f, g, h);
    }
    public static void uniform3i(int programId, String name, int f, int g, int h){
        glUniform3i(getLocation(programId,name),f,h,g);
    }

    public static void uniform4f(int programId, String name, float f, float g, float h, float i) {
        glUniform4f(getLocation(programId, name), f, g, h, i);
    }

    public static void uniformMatrix4f(int programId, String name, Matrix4f matrix) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(16);
            matrix.get(buffer);
            glUniformMatrix4fv(getLocation(programId, name), false, buffer);
        }
    }
}