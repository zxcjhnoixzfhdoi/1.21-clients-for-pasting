#version 330 core

layout(location = 0) in vec3 position;
layout(location = 1) in float size;
layout(location = 2) in uint color;
layout(location = 3) in vec4 uvRect;
layout(location = 4) in float rotation;

layout(std140) uniform Camera {
    mat4 view;
    mat4 proj;
    vec3 origin;
};

out vec2 vUV;
out vec4 vColor;

const vec2 vertices[4] = vec2[](
    vec2(-0.5, -0.5),
    vec2(-0.5,  0.5),
    vec2( 0.5, -0.5),
    vec2( 0.5,  0.5)
);

void main() {
    vec3 right = vec3(view[0][0], view[1][0], view[2][0]);
    vec3 up    = vec3(view[0][1], view[1][1], view[2][1]);

    vec2 c = vertices[gl_VertexID % 4] * size;
    float cosR = cos(rotation);
    float sinR = sin(rotation);
    vec2 corner = vec2(c.x * cosR - c.y * sinR, c.x * sinR + c.y * cosR);
    vec3 worldPos = position + right * corner.x + up * corner.y;

    float a = float((color >> 24u) & 0xFFu) / 255.0;
    float r = float((color >> 16u) & 0xFFu) / 255.0;
    float g = float((color >> 8u)  & 0xFFu) / 255.0;
    float b = float(color & 0xFFu)           / 255.0;

    vec2 corner01 = vertices[gl_VertexID % 4] + 0.5;
    vColor = vec4(r, g, b, a);
    vUV = uvRect.xy + corner01 * (uvRect.zw - uvRect.xy);
    gl_Position = proj * view * vec4(worldPos, 1.0);
}
