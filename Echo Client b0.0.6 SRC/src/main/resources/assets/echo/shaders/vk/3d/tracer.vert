#version 450

layout(location = 0) in vec3 pos;
layout(location = 1) in int color;

layout(location = 0) out vec4 vColor;

layout(set = 0, binding = 0) uniform Camera {
    mat4 view;
    mat4 proj;
    vec3 origin;
} ubo;

void main() {
    float a = float((color >> 24) & 0xFF) / 255.0;
    float r = float((color >> 16) & 0xFF) / 255.0;
    float g = float((color >> 8) & 0xFF) / 255.0;
    float b = float(color & 0xFF) / 255.0;
    vColor = vec4(r, g, b, a);

    vec4 worldPos = ubo.proj * ubo.view * vec4(pos, 1.0);
    if (worldPos.w < 0.0) {
        vec2 dir = worldPos.xy;
        float m = max(abs(dir.x), abs(dir.y));
        if (m < 1e-6) dir = vec2(0.0, 1.0);
        else dir /= m;
        worldPos = vec4(dir, 0.0, 1.0);
    }
    gl_Position = mix(
        worldPos,
        vec4(0.0, 0.0, 0.0, 1.0),
        float(gl_VertexIndex & 1)
    );
}