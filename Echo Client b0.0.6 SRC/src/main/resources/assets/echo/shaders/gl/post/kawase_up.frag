#version 330

uniform float offset;
uniform sampler2D tex;

in vec2 uv;
in vec2 texelSize;

out vec4 fragColor;

void main() {
    vec2 halfpixel = texelSize / 2.0;

    vec4 sum = texture(tex, uv + vec2(-halfpixel.x * 2.0, 0.0) * offset);
    sum += texture(tex, uv + vec2(-halfpixel.x, halfpixel.y) * offset) * 2.0;
    sum += texture(tex, uv + vec2(0.0, halfpixel.y * 2.0) * offset);
    sum += texture(tex, uv + vec2(halfpixel.x, halfpixel.y) * offset) * 2.0;
    sum += texture(tex, uv + vec2(halfpixel.x * 2.0, 0.0) * offset);
    sum += texture(tex, uv + vec2(halfpixel.x, -halfpixel.y) * offset) * 2.0;
    sum += texture(tex, uv + vec2(0.0, -halfpixel.y * 2.0) * offset);
    sum += texture(tex, uv + vec2(-halfpixel.x, -halfpixel.y) * offset) * 2.0;

    fragColor = sum / 12.0;
}