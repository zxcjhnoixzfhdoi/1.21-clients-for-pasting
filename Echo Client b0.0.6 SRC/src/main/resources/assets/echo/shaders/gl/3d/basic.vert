#version 330 core

layout(location = 0) in vec3 position;
layout(location = 1) in int color;

layout(std140) uniform Camera {
    mat4 view;
    mat4 proj;
    vec3 origin;
};

out vec4 vertexColor;

void main() {
    float a = ((color >> 24) & 0xFF) / 255.0;
    float r = ((color >> 16) & 0xFF) / 255.0;
    float g = ((color >> 8) & 0xFF) / 255.0;
    float b = (color & 0xFF) / 255.0;
    vertexColor = vec4(r, g, b, a);
    gl_Position = proj * view * vec4(position, 1.0);
}
