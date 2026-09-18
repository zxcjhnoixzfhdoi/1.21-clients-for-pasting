#version 330 core

uniform mat4 modelMat;
uniform vec4 rect;
uniform vec4 radius;
uniform float z;
out vec2 vCoord;
out vec2 vUV;
out vec2 vSize;
out vec4 vRadius;

layout(std140) uniform Camera {
    mat4 proj;
};

const vec2[4] quad = vec2[](
    vec2(0.0, 0.0),
    vec2(1.0, 0.0),
    vec2(0.0, 1.0),
    vec2(1.0, 1.0)
);

void main() {
    int id = gl_VertexID % 4;

    vCoord = quad[id];
    vUV = quad[id];
    vSize = rect.zw;
    vRadius = radius;

    vec2 local = (vCoord * rect.zw) + rect.xy;
    gl_Position = proj * modelMat * vec4(local, z, 1.0);
}