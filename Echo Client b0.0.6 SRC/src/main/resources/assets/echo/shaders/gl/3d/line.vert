#version 330 core

layout(location = 0) in vec3 from;
layout(location = 1) in vec3 to;
layout(location = 2) in int color;

out vec4 vertexColor;

layout(std140) uniform Camera {
    mat4 view;
    mat4 proj;
    vec3 origin;
};


vec4 getColor(int c) {
    float a = float((c >> 24) & 0xFF) / 255.0;
    float r = float((c >> 16) & 0xFF) / 255.0;
    float g = float((c >> 8) & 0xFF) / 255.0;
    float b = float(c & 0xFF) / 255.0;
    return vec4(r, g, b, a);
}

void main() {
    vec3 pos = mix(from, to, gl_VertexID & 1);
    vertexColor = getColor(color);
    gl_Position = proj * view * vec4(pos, 1.0);
}
