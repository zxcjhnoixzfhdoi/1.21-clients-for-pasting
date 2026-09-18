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
    vSize = rect.zw;
    vRadius = radius;

    vec2 local = rect.xy + (quad[id] * rect.zw);
    vec2 transformed = (model * vec4(local, 0.0, 1.0)).xy;
    vec2 screenSize = vec2(2.0 / ubo.proj[0][0], 2.0 / abs(ubo.proj[1][1]));

    vUV = vec2(transformed.x / screenSize.x, transformed.y / screenSize.y);
    vAlpha = alpha;
    vTexIndex = texIndex;
    gl_Position = ubo.proj * vec4(transformed, z, 1.0);
}
