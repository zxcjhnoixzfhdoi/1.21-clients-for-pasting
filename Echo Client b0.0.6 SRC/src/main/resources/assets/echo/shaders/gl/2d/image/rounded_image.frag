#version 330 core

in vec2 vCoord;
in vec2 vUV;
in vec2 vSize;
in vec4 vRadius;

uniform sampler2D Sampler0;
uniform float uAlpha;

out vec4 fragColor;

float sdf(in vec2 p, in vec2 b, in vec4 r) {
    r.xy = (p.x > 0.0) ? r.xz : r.yw;
    r.x  = (p.y > 0.0) ? r.x  : r.y;
    vec2 q = abs(p) - b + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

void main() {
    vec2 p = vSize * 0.5;
    float dist = sdf(p - (vCoord * vSize), p, vRadius);
    float edge = fwidth(dist) * 0.5;
    float alpha = smoothstep(edge, -edge, dist);

    if (alpha <= 0.01) discard;

    vec4 texColor = texture(Sampler0, vUV);
    fragColor = vec4(texColor.rgb, texColor.a * alpha * uAlpha);
}
