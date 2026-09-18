#version 330


uniform sampler2D tex;

out vec2 uv;
out vec2 texelSize;
void main() {
    texelSize = 1.0 / textureSize(tex, 0);
    uv = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
    gl_Position = vec4(uv * 2.0 - 1.0, 0.0, 1.0);
}
