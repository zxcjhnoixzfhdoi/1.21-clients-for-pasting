package hack.echo.client.render2.impl.opengl.font;

import hack.echo.client.render2.impl.vulkan.impl.VkText;
import hack.echo.client.utils.VulkanUtil;
import hack.echo.client.vulkan.descriptor.SimpleUBO;
import lombok.Getter;

public class Fonts {

    public static Font arial;
    public static Font inter;
    public static Font interSemiBold;
    public static Font lucide;

    public static void init() {
        try {
            arial = new Font("arial");
            inter = new Font("inter");
            interSemiBold = new Font("inter_semi_bold");
            lucide = new Font("lucide");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void destroy() {
        arial.destroy();
        inter.destroy();
        interSemiBold.destroy();
        lucide.destroy();
    }
}
