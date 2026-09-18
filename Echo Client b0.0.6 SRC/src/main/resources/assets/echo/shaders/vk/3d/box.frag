#version 450

layout(location = 0) in vec4 vColor;
layout(location = 0) out vec4 fragColor;


const float lineWidth = 1.5;

void main() {
    fragColor = vColor;
}
