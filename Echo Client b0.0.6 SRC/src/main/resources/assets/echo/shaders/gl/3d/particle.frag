#version 330 core

in vec2 vUV;
in vec4 vColor;

uniform sampler2D Sampler0;

out vec4 fragColor;

void main() {
    vec4 texColor = texture(Sampler0, vUV);
    float a = texColor.a * vColor.a;
    fragColor = vec4(texColor.rgb * vColor.rgb * a, a);
}
