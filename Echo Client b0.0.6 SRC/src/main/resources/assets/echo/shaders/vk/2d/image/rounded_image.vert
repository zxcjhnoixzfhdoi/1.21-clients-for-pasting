#version 450

layout(location = 0) in mat4 model;
layout(location = 4) in vec4 rect;
layout(location = 5) in vec4 radius;
layout(location = 6) in float alpha;
layout(location = 7) in int texIndex;
layout(location = 8) in float z;

layout(set = 0, binding = 0) uniform Camera {
    mat4 proj;
} ubo;

layout(location = 0) out vec2 vCoord;
layout(location = 1) out vec2 vUV;
layout(location = 2) out vec2 vSize;
layout(location = 3) out vec4 vRadius;
layout(location = 4) out float vAlpha;
layout(location = 5) flat out int vTexIndex;

const vec2[4] quad = vec2[](
    vec2(0.0, 0.0),
    vec2(1.0, 0.0),
    vec2(0.0, 1.0),
    vec2(1.0, 1.0)
);

void main() {
    int id = gl_VertexIndex % 4;

    vCoord = quad[id];
    vUV = quad[id];
    vSize = rect.zw;
    vRadius = radius;
    vAlpha = alpha;
    vTexIndex = texIndex;
    vec2 local = (vCoord * rect.zw) + rect.xy;
    gl_Position = ubo.proj * model * vec4(local, z, 1.0);
}
