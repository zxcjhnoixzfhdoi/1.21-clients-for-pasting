#version 330 core

layout(location = 0) in uint packedData;
layout(location = 1) in vec4 chunkPos;

layout(std140) uniform Camera {
    mat4 view;
    mat4 proj;
    vec3 origin;
};
uniform usamplerBuffer palette;
const vec3 vertices[4] = vec3[](
    vec3(0.0, 0.0, 0.0),
    vec3(0.0, 0.0, 1.0),
    vec3(1.0, 0.0, 0.0),
    vec3(1.0, 0.0, 1.0)
);

out vec4 vertexColor;
out vec3 worldPos;
flat out int faceDir;

vec4 colorFromId(uint id) {
    uint argb = texelFetch(palette, int(id)).r;
    float a = float((argb >> 24u) & 0xFFu) / 255.0;
    float r = float((argb >> 16u) & 0xFFu) / 255.0;
    float g = float((argb >> 8u) & 0xFFu) / 255.0;
    float b = float(argb & 0xFFu) / 255.0;
    return vec4(r, g, b, a);
}

void main() {
    uint blockId = (packedData >> 20u) & 0xFFFu;

    float x = float((packedData >> 0u) & 0xFu);
    float y = float((packedData >> 4u) & 0xFu);
    float z = float((packedData >> 8u) & 0xFu);
    float w = float((packedData >> 12u) & 0xFu) + 1.0;
    float h = float((packedData >> 16u) & 0xFu) + 1.0;
    int dir = int(chunkPos.w);

    vec3 model = vertices[gl_VertexID % 4];

    bool winding = (0x29 & (1 << dir)) != 0;

    model.xyz = winding ? model.zyx : model.xyz;


    model.x *= w;
    model.z *= h;

    if (dir == 1) {
        model.y++;
    } else if (dir == 2) {
        model.xyz = model.yxz;
    } else if (dir == 3) {
        model.xyz = model.yxz;
        model.x++;
    } else if (dir == 4) {
        model.xyz = model.xzy;
    } else if (dir == 5) {
        model.xyz = model.xzy;
        model.z++;
    }

    vertexColor = colorFromId(blockId);
    worldPos = chunkPos.xyz + model + vec3(x, y, z) + fract(origin);
    faceDir = dir;
    gl_Position = proj * view * vec4(chunkPos.xyz + model + vec3(x, y, z), 1.0);
}
