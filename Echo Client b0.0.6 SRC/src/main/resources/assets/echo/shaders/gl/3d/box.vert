#version 330 core

layout(location = 0) in vec3 position;
layout(location = 1) in vec3 size;
layout(location = 2) in uint color;

layout(std140) uniform Camera {
    mat4 view;
    mat4 proj;
    vec3 origin;
};

out vec4 vertexColor;

const vec3 vertices[6] = vec3[](
    vec3(0.0, 0.0, 0.0),
    vec3(0.0, 0.0, 1.0),
    vec3(1.0, 0.0, 0.0),
    vec3(0.0, 0.0, 1.0),
    vec3(1.0, 0.0, 1.0),
    vec3(1.0, 0.0, 0.0)
);

void main() {
    int face = gl_VertexID / 6;
    vec3 model = vertices[gl_VertexID % 6];

    if (face == 0 || face == 3 || face == 5) {
        model.xyz = model.zyx;
    }

    if (face == 0) {

    } else if (face == 1) {
        model.y++;
    } else if (face == 2) {
        model.xyz = model.yxz;
    } else if (face == 3) {
        model.xyz = model.yxz;
        model.x++;
    } else if (face == 4) {
        model.xyz = model.xzy;
    } else if (face == 5) {
        model.xyz = model.xzy;
        model.z++;
    }

    float a = float((color >> 24u) & 0xFFu) / 255.0;
    float r = float((color >> 16u) & 0xFFu) / 255.0;
    float g = float((color >> 8u) & 0xFFu) / 255.0;
    float b = float(color & 0xFFu) / 255.0;

    vertexColor = vec4(r, g, b, a);
    gl_Position = proj * view * vec4(position + model * size, 1.0);
}
