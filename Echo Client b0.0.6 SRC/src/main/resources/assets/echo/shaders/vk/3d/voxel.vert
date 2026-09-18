#version 460

struct MeshData {
    uint count;
    uint base;
};

struct ChunkMesh {
    ivec4 pos;
    MeshData faces[6];
};

layout(set = 0, binding = 0) uniform Camera {
    mat4 view;
    mat4 proj;
    vec3 origin;
} ubo;

layout(std430, set = 0, binding = 1) readonly buffer ChunkMeshes {
    ChunkMesh chunks[];
};

layout(set = 0, binding = 2) uniform usamplerBuffer palette;

layout(push_constant) uniform PushConstants {
    int fill;
    int outline;
    int camChunkX;
    int camChunkY;
    int camChunkZ;
    float camLocalX;
    float camLocalY;
    float camLocalZ;
    int chunk_count;
};

layout(location = 0) in uint data;

layout(location = 0) out vec4 vertex_color;
layout(location = 1) out vec3 world_pos;
layout(location = 2) out flat int face_dir;
layout(location = 3) out flat int out_fill;
layout(location = 4) out flat int out_outline;


const vec3 vertices[4] = vec3[](
    vec3(0.0, 0.0, 0.0),
    vec3(0.0, 0.0, 1.0),
    vec3(1.0, 0.0, 0.0),
    vec3(1.0, 0.0, 1.0)
);

vec4 get_color(int id) {
    uint c = texelFetch(palette, id).r;
    float a = float((c >> 24) & 0xFF) / 255.0;
    float r = float((c >> 16) & 0xFF) / 255.0;
    float g = float((c >> 8) & 0xFF) / 255.0;
    float b = float(c & 0xFF) / 255.0;
    return vec4(r, g, b, a);
};

vec3 get_relative_pos(ivec3 chunkPos) {
    ivec3 relChunk = chunkPos - ivec3(camChunkX, camChunkY, camChunkZ);
    return vec3(relChunk * 16) - vec3(camLocalX, camLocalY, camLocalZ);
}

vec3 get_model(int vertex, int face, float w, float h) {
    vec3 model = vertices[vertex];
    bool winding = (0x29 & (1 << face)) != 0;

    model.xyz = winding ? model.zyx : model.xyz;

    model.x *= w;
    model.z *= h;
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


    return model;
}

void main() {
    int id = gl_VertexIndex / 4;
    int chunk_id = id / 6;
    int face = id % 6;

    float x = float(data & 0xF);
    float y = float((data >> 4) & 0xF);
    float z = float((data >> 8) & 0xF);
    float w = float((data >> 12) & 0xF) + 1.0;
    float h = float((data >> 16) & 0xF) + 1.0;
    int block_id = int((data >> 20) & 0xFFF);

    vec3 local_pos = get_model(gl_VertexIndex % 4, face, w, h) + vec3(x, y, z);

    vec3 chunk_pos = get_relative_pos(chunks[chunk_id].pos.xyz);


    vertex_color = get_color(block_id);
    world_pos = vec3(chunks[chunk_id].pos.xyz * 16) + floor(local_pos);
    face_dir = face;
    out_fill = fill;
    out_outline = outline;
    gl_Position = ubo.proj * ubo.view * vec4(chunk_pos + local_pos, 1.0);
}




