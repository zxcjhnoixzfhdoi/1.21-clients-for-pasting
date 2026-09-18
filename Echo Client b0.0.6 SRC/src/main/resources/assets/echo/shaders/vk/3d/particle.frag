#version 450

layout(location = 0) in vec2 vUV;
layout(location = 1) in vec4 vColor;

layout(set = 0, binding = 1) uniform sampler2D tex;

layout(location = 0) out vec4 outColor;

void main() {
    vec4 texColor = texture(tex, vUV);
    float a = texColor.a * vColor.a;
    outColor = vec4(texColor.rgb * vColor.rgb * a, a);
}
