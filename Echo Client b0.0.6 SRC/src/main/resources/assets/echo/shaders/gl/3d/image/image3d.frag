#version 330 core

in vec2 vUV;
in vec4 colorMul;
uniform sampler2D Sampler0;

out vec4 fragColor;

void main() {
    vec4 texColor = texture(Sampler0, vUV);
    float a = texColor.a * colorMul.a;
    if (a <= 0.01) discard;
    fragColor = vec4(texColor.rgb * colorMul.rgb * a, a);
}
