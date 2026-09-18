#version 450

layout(location = 0) in mat4 model;
layout(location = 4) in vec4 rect;
layout(location = 5) in vec3 baseColor;
layout(location = 6) in float z;

layout(set = 0, binding = 0) uniform Camera {
    mat4 proj;
} ubo;

layout(location = 0) out vec2 vUV;
layout(location = 1) out vec3 vBaseColor;

const vec2[4] quad = vec2[](
    vec2(0.0, 0.0),
    vec2(1.0, 0.0),
    vec2(0.0, 1.0),
    vec2(1.0, 1.0)
);

void main() {
    int id = gl_VertexIndex % 4;
    vUV = quad[id];
    vBaseColor = baseColor;
    vec2 local = (vUV * rect.zw) + rect.xy;
    gl_Position = ubo.proj * model * vec4(local, z, 1.0);
}
