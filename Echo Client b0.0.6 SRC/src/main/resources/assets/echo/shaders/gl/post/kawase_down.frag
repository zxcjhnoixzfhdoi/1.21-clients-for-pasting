#version 330

uniform float offset;
uniform sampler2D tex;

in vec2 uv;
in vec2 texelSize;

out vec4 fragColor;

void main() {
    vec2 halfpixel = texelSize / 2.0;

    vec4 sum = texture(tex, uv) * 4.0;
    sum += texture(tex, uv - halfpixel.xy * offset);
    sum += texture(tex, uv + halfpixel.xy * offset);
    sum += texture(tex, uv + vec2(halfpixel.x, -halfpixel.y) * offset);
    sum += texture(tex, uv - vec2(halfpixel.x, -halfpixel.y) * offset);

    fragColor = sum / 8.0;
}