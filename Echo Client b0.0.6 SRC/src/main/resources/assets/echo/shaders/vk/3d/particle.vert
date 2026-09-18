#version 450

layout(location = 0) in vec3 position;
layout(location = 1) in float size;
layout(location = 2) in int color;
layout(location = 3) in vec4 uvRect;
layout(location = 4) in float rotation;

layout(set = 0, binding = 0) uniform Camera {
    mat4 view;
    mat4 proj;
    vec3 origin;
} ubo;

layout(location = 0) out vec2 vUV;
layout(location = 1) out vec4 vColor;

const vec2 vertices[4] = vec2[](
    vec2(-0.5, -0.5),
    vec2(-0.5,  0.5),
    vec2( 0.5, -0.5),
    vec2( 0.5,  0.5)
);

void main() {
    vec3 right = vec3(ubo.view[0][0], ubo.view[1][0], ubo.view[2][0]);
    vec3 up    = vec3(ubo.view[0][1], ubo.view[1][1], ubo.view[2][1]);

    vec2 c = vertices[gl_VertexIndex % 4] * size;
    float cosR = cos(rotation);
    float sinR = sin(rotation);
    vec2 corner = vec2(c.x * cosR - c.y * sinR, c.x * sinR + c.y * cosR);
    vec3 worldPos = position + right * corner.x + up * corner.y;

    float a = float((color >> 24) & 0xFF) / 255.0;
    float r = float((color >> 16) & 0xFF) / 255.0;
    float g = float((color >> 8) & 0xFF) / 255.0;
    float b = float(color & 0xFF) / 255.0;

    vec2 corner01 = vertices[gl_VertexIndex % 4] + 0.5;
    vColor = vec4(r, g, b, a);
    vUV = uvRect.xy + corner01 * (uvRect.zw - uvRect.xy);
    gl_Position = ubo.proj * ubo.view * vec4(worldPos, 1.0);
}
