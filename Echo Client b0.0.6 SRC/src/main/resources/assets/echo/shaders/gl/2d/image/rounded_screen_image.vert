#version 330 core

uniform mat4 modelMat;
uniform vec4 rect;
uniform vec4 radius;
uniform float z;
out vec2 vCoord;
out vec2 vUV;
out vec2 vSize;
out vec4 vRadius;

const vec2[4] quad = vec2[](
    vec2(0.0, 0.0),
    vec2(1.0, 0.0),
    vec2(0.0, 1.0),
    vec2(1.0, 1.0)
);

layout(std140) uniform Camera {
    mat4 proj;
};

void main() {
    int id = gl_VertexID % 4;

    vCoord = quad[id];
    vSize = rect.zw;
    vRadius = radius;

    vec2 local = rect.xy + (quad[id] * rect.zw);
    vec2 transformed = (modelMat * vec4(local, 0.0, 1.0)).xy;
    vec2 screenSize = vec2(2.0 / proj[0][0], 2.0 / abs(proj[1][1]));

    vUV = vec2(transformed.x / screenSize.x, 1.0 - (transformed.y / screenSize.y));
    gl_Position = proj * vec4(transformed, z, 1.0);
}