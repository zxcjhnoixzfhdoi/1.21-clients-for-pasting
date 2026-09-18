#version 450

layout(location = 0) in vec3 from;
layout(location = 1) in vec3 to;
layout(location = 2) in int color;

layout(location = 0) out vec4 vColor;

layout(set = 0, binding = 0) uniform Camera {
    mat4 view;
    mat4 proj;
    vec3 origin;
} ubo;

vec4 getColor(int c) {
    float a = float((c >> 24) & 0xFF) / 255.0;
    float r = float((c >> 16) & 0xFF) / 255.0;
    float g = float((c >> 8) & 0xFF) / 255.0;
    float b = float(c & 0xFF) / 255.0;
    return vec4(r, g, b, a);
}

void main() {
    vec3 pos = mix(from, to, float(gl_VertexIndex & 1));
    vColor = getColor(color);
    gl_Position = ubo.proj * ubo.view * vec4(pos, 1.0);
}