#version 450

layout(location = 0) in vec2 vUV;
layout(location = 1) in vec4 colorMul;

layout(set = 0, binding = 1) uniform sampler2D tex;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 texColor = texture(tex, vUV);
    float a = texColor.a * colorMul.a;
    if (a <= 0.01) discard;
    fragColor = vec4(texColor.rgb * colorMul.rgb * a, a);
}
