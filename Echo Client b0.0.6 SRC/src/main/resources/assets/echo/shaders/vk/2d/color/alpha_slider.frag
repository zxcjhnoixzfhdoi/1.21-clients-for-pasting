#version 450

layout(location = 0) in vec2 vUV;
layout(location = 1) in vec3 vBaseColor;

layout(location = 0) out vec4 fragColor;

void main() {
    fragColor = vec4(vBaseColor, vUV.x);
}
