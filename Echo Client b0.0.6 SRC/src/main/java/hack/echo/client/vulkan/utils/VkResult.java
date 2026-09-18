package hack.echo.client.vulkan.utils;

import org.lwjgl.vulkan.VK10;

import static org.lwjgl.vulkan.VK10.*;

public class VkResult {

    public static String decode(int result) {
        String res;
        switch (result) {
            case VK_ERROR_UNKNOWN -> res = "VK_ERROR_UNKNOWN";
            case VK_ERROR_FRAGMENTED_POOL -> res = "VK_ERROR_FRAGMENTED_POOL";
            case VK_ERROR_FORMAT_NOT_SUPPORTED -> res = "VK_ERROR_FORMAT_NOT_SUPPORTED";
            case VK_ERROR_TOO_MANY_OBJECTS -> res = "VK_ERROR_TOO_MANY_OBJECTS";
            case VK_ERROR_INCOMPATIBLE_DRIVER -> res = "VK_ERROR_INCOMPATIBLE_DRIVER";
            case VK_ERROR_FEATURE_NOT_PRESENT -> res = "VK_ERROR_FEATURE_NOT_PRESENT";
            case VK_ERROR_EXTENSION_NOT_PRESENT -> res = "VK_ERROR_EXTENSION_NOT_PRESENT";
            case VK_ERROR_LAYER_NOT_PRESENT -> res = "VK_ERROR_LAYER_NOT_PRESENT";
            case VK_ERROR_MEMORY_MAP_FAILED -> res = "VK_ERROR_MEMORY_MAP_FAILED";
            case VK_ERROR_DEVICE_LOST -> res = "VK_ERROR_DEVICE_LOST";
            case VK_ERROR_INITIALIZATION_FAILED -> res = "VK_ERROR_INITIALIZATION_FAILED";
            case VK_ERROR_OUT_OF_DEVICE_MEMORY -> res = "VK_ERROR_OUT_OF_DEVICE_MEMORY";
            case VK_ERROR_OUT_OF_HOST_MEMORY -> res = "VK_ERROR_OUT_OF_HOST_MEMORY";
            case VK_SUCCESS -> res = "VK_SUCCESS";
            case VK_NOT_READY -> res = "VK_NOT_READY";
            case VK_TIMEOUT -> res = "VK_TIMEOUT";
            case VK_EVENT_SET -> res = "VK_EVENT_SET";
            case VK_EVENT_RESET -> res = "VK_EVENT_RESET";
            case VK_INCOMPLETE -> res = "VK_INCOMPLETE";
            default -> res = Integer.toString(result);
        }

        return res;
    }
}
