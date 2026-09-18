#version 330 core
layout(location = 0) in vec3 pos;
layout(location = 1) in uint color;
layout(std140) uniform Camera {
    mat4 view;
    mat4 proj;
    vec3 origin;
};
out vec4 vertexColor;
void main() {
    float a = float((color >> 24u) & 0xFFu) / 255.0;
    float r = float((color >> 16u) & 0xFFu) / 255.0;
    float g = float((color >> 8u) & 0xFFu) / 255.0;
    float b = float(color & 0xFFu) / 255.0;
    vertexColor = vec4(r, g, b, a);

    float isCursor = float(gl_VertexID & 1);
    vec4 worldPos = proj * view * vec4(pos, 1.0);
    if (worldPos.w < 0.0) {
        vec2 dir = worldPos.xy;
        float m = max(abs(dir.x), abs(dir.y));
        if (m < 1e-6) dir = vec2(0.0, 1.0);
        else dir /= m;
        worldPos = vec4(dir, 0.0, 1.0);
    }
    vec4 cursorPos = vec4(0.0, 0.0, 0.0, 1.0);
    gl_Position = mix(worldPos, cursorPos, isCursor);
}
