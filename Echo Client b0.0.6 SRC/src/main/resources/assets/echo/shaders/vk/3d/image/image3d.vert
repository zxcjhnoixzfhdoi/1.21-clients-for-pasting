#version 450

layout(location = 0) in vec4 col0;
layout(location = 1) in vec4 col1;
layout(location = 2) in vec4 col2;
layout(location = 3) in vec4 col3;
layout(location = 4) in int color;

layout(set = 0, binding = 0) uniform Camera {
    mat4 view;
    mat4 proj;
    vec3 origin;
} ubo;

layout(push_constant) uniform PushConstants {
    float width;
    float height;
} pc;

layout(location = 0) out vec2 vUV;
layout(location = 1) out vec4 colorMul;

const vec2[4] quad = vec2[](
    vec2(0.0, 0.0),
    vec2(1.0, 0.0),
    vec2(0.0, 1.0),
    vec2(1.0, 1.0)
);

void main() {
    int id = gl_VertexIndex % 4;

    float a = float((color >> 24) & 0xFF) / 255.0;
    float r = float((color >> 16) & 0xFF) / 255.0;
    float g = float((color >> 8) & 0xFF) / 255.0;
    float b = float(color & 0xFF) / 255.0;
    colorMul = vec4(r, g, b, a);

    mat4 model = mat4(col0, col1, col2, col3);
    vec3 localPos = vec3((quad[id] - 0.5) * vec2(pc.width, pc.height), 0.0);

    vUV = vec2(quad[id].x, 1.0 - quad[id].y);
    gl_Position = ubo.proj * ubo.view * model * vec4(localPos, 1.0);
}
