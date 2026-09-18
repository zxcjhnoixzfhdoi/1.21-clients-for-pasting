#version 330 core

in vec2 vUV;
flat in vec3 vBaseColor;

out vec4 fragColor;

void main() {
    fragColor = vec4(vBaseColor, vUV.x);
}
