#version 330 core

layout(location = 0) in mat4 modelMat;
layout(location = 4) in vec4 rect;
layout(location = 5) in vec4 radius;
layout(location = 6) in int tint;
layout(location = 7) in float refractionStrength;
layout(location = 8) in float z;

layout(std140) uniform Camera {
    mat4 proj;
};

out vec2 vCoord;
out vec2 vUV;
out vec2 vSize;
out vec4 vRadius;
out vec4 vTint;
out float vRefractStrength;

const vec2[4] quad = vec2[](
    vec2(0.0, 0.0),
    vec2(1.0, 0.0),
    vec2(0.0, 1.0),
    vec2(1.0, 1.0)
);

vec4 unpackColor(int c) {
    float a = float((c >> 24) & 0xFF) / 255.0;
    float r = float((c >> 16) & 0xFF) / 255.0;
    float g = float((c >> 8)  & 0xFF) / 255.0;
    float b = float(c         & 0xFF) / 255.0;
    return vec4(r, g, b, a);
}

void main() {
    int id = gl_VertexID % 4;

    vCoord = quad[id];
    vSize = rect.zw;
    vRadius = radius;
    vTint = unpackColor(tint);
    vRefractStrength = refractionStrength;

    vec2 local = rect.xy + (quad[id] * rect.zw);
    vec2 transformed = (modelMat * vec4(local, 0.0, 1.0)).xy;
    vec2 screenSize = vec2(2.0 / proj[0][0], 2.0 / abs(proj[1][1]));

    vUV = vec2(transformed.x / screenSize.x, 1.0 - (transformed.y / screenSize.y));
    gl_Position = proj * vec4(transformed, z, 1.0);
}
