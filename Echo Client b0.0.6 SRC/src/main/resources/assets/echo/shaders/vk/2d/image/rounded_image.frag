#version 450
#extension GL_EXT_nonuniform_qualifier : require

layout(location = 0) in vec2 vCoord;
layout(location = 1) in vec2 vUV;
layout(location = 2) in vec2 vSize;
layout(location = 3) in vec4 vRadius;
layout(location = 4) in float vAlpha;
layout(location = 5) flat in int vTexIndex;

layout(set = 0, binding = 1) uniform sampler2D samplers[];

layout(location = 0) out vec4 fragColor;

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

    vec4 texColor = texture(samplers[nonuniformEXT(vTexIndex)], vUV);
    fragColor = vec4(texColor.rgb, texColor.a * alpha * vAlpha);
}
