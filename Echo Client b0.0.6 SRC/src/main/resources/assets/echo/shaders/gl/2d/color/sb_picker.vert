#version 330 core

layout(location = 0) in mat4 modelMat;
layout(location = 4) in vec4 rect;
layout(location = 5) in float hue;
layout(location = 6) in float z;

layout(std140) uniform Camera {
    mat4 proj;
};
out vec2 vUV;
flat out float vHue;

const vec2[4] quad = vec2[](
    vec2(0.0, 0.0),
    vec2(1.0, 0.0),
    vec2(0.0, 1.0),
    vec2(1.0, 1.0)
);

void main() {
    int id = gl_VertexID % 4;
    vUV = quad[id];
    vHue = hue;
    vec2 local = (vUV * rect.zw) + rect.xy;
    gl_Position = proj * modelMat * vec4(local, z, 1.0);
}
