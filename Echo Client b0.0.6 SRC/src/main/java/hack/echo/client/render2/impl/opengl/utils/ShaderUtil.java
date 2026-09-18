package hack.echo.client.render2.impl.opengl.utils;

import hack.echo.client.utils.ResourceHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

public class ShaderUtil {

    public static int createShader(String fragmentResource, String vertexResource) {
        String fragmentSource = getShaderResource(fragmentResource);
        String vertexSource = getShaderResource(vertexResource);

        if (fragmentSource == null || vertexSource == null) {
            return -1;
        }

        int fragmentId = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        int vertexId = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);

        GL20.glShaderSource(fragmentId, fragmentSource);
        GL20.glShaderSource(vertexId, vertexSource);
        GL20.glCompileShader(fragmentId);
        GL20.glCompileShader(vertexId);

        if (isShaderInvalid(fragmentId, fragmentResource)) return -1;
        if (isShaderInvalid(vertexId, vertexResource)) return -1;

        int programId = GL20.glCreateProgram();
        GL20.glAttachShader(programId, fragmentId);
        GL20.glAttachShader(programId, vertexId);
        GL20.glValidateProgram(programId);
        GL20.glLinkProgram(programId);
        GL20.glDeleteShader(fragmentId);
        GL20.glDeleteShader(vertexId);

        return programId;
    }


    private static boolean isShaderInvalid(int shaderId, String source) {
        boolean compiled = GL20.glGetShaderi(shaderId, GL20.GL_COMPILE_STATUS) == GL11.GL_TRUE;
        if (compiled) return false;

        String shaderLog = GL20.glGetShaderInfoLog(shaderId, 8192);
        System.out.println("\nError while compiling shader: " + source);
        System.out.println("-------------------------------");
        System.out.println(shaderLog);
        return true;
    }

    public static String getShaderResource(String resource) {
        return ResourceHelper.getString("shaders/" + resource);
    }


}
