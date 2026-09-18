#version 450

layout(location = 0) in vec2 vUV;
layout(location = 1) in float vHue;

layout(location = 0) out vec4 fragColor;

vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

void main() {
    float sat = vUV.x;
    float bri = 1.0 - vUV.y;
    vec3 rgb = hsv2rgb(vec3(vHue, sat, bri));
    fragColor = vec4(rgb, 1.0);
}
