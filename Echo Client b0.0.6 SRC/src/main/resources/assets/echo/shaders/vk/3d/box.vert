#version 450

layout(location = 0) in vec3 position;
layout(location = 1) in vec3 size;
layout(location = 2) in int color;

layout(set = 0, binding = 0) uniform Camera {
    mat4 view;
    mat4 proj;
    vec3 origin;
} ubo;

layout(location = 0) out vec4 vColor;

const vec3 vertices[6] = vec3[](
    vec3(0.0, 0.0, 0.0),
    vec3(0.0, 0.0, 1.0),
    vec3(1.0, 0.0, 0.0),
    vec3(0.0, 0.0, 1.0),
    vec3(1.0, 0.0, 1.0),
    vec3(1.0, 0.0, 0.0)
);

void main() {
    int face = gl_VertexIndex / 6;
    vec3 model = vertices[gl_VertexIndex % 6];

    bool winding = (0x29 & (1 << face)) != 0;
    model.xyz = winding ? model.zyx : model.xyz;

    if (face == 1) {
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

    float a = float((color >> 24) & 0xFF) / 255.0;
    float r = float((color >> 16) & 0xFF) / 255.0;
    float g = float((color >> 8) & 0xFF) / 255.0;
    float b = float(color & 0xFF) / 255.0;

    vColor = vec4(r, g, b, a);
    gl_Position = ubo.proj * ubo.view * vec4(position + model * size, 1.0);
}
