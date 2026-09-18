package hack.echo.client.vulkan.compute;

import hack.echo.client.vulkan.CommandBuffer;

public interface ComputeDispatch {
    void executeCompute(CommandBuffer cmd);
    void cleanup();
}
