#version 450

layout(location = 0) in mat4 model;
layout(location = 4) in vec4 rect;
layout(location = 5) in vec4 uv;
layout(location = 6) in ivec4 color;
layout(location = 7) in int texIndex;
layout(location = 8) in float z;

layout(set = 0, binding = 0) uniform Camera {
    mat4 proj;
} ubo;

layout(location = 0) out vec2 vUV;
layout(location = 1) out vec4 vColor;
layout(location = 2) flat out int vTexIndex;

const vec2[4] quad = vec2[](
    vec2(0.0, 0.0),
    vec2(1.0, 0.0),
    vec2(0.0, 1.0),
    vec2(1.0, 1.0)
);

vec4 getColor(int c) {
    float a = float((c >> 24) & 0xFF) / 255.0;
    float r = float((c >> 16) & 0xFF) / 255.0;
    float g = float((c >> 8) & 0xFF) / 255.0;
    float b = float(c & 0xFF) / 255.0;
    return vec4(r, g, b, a);
}

void main() {
    int id = gl_VertexIndex % 4;
    vec2 q = quad[id];
    
    vec2 pos = rect.xy + q * rect.zw;
    vUV = uv.xy + q * uv.zw;
    vColor = getColor(color[id]);
    vTexIndex = texIndex;
    gl_Position = ubo.proj * model * vec4(pos, z, 1.0);
}
