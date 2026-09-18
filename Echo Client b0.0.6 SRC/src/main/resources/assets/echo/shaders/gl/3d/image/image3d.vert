#version 330 core


layout(location = 0) in mat4 model;
layout(location = 4) in uint color;


layout(std140) uniform Camera {
    mat4 view;
    mat4 proj;
    vec3 origin;
};

uniform vec2 size;

out vec2 vUV;
out vec4 colorMul;

const vec2[4] quad = vec2[](
    vec2(0.0, 0.0),
    vec2(1.0, 0.0),
    vec2(0.0, 1.0),
    vec2(1.0, 1.0)
);

void main() {
    int id = gl_VertexID % 4;

    float a = float((color >> 24u) & 0xFFu) / 255.0;
    float r = float((color >> 16u) & 0xFFu) / 255.0;
    float g = float((color >> 8u) & 0xFFu) / 255.0;
    float b = float(color & 0xFFu) / 255.0;



    vec3 localPos = vec3((quad[id] - 0.5) * size, 0.0);

    gl_Position = proj * view * model * vec4(localPos, 1.0);

    vUV = vec2(quad[id].x, 1.0 - quad[id].y);
    colorMul = vec4(r,g,b,a);
}
