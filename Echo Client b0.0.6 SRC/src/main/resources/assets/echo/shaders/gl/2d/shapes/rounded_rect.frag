#version 330 core

in vec2 vCoord;
in vec4 vColor;
in vec2 vSize;
in vec4 vRadius;

out vec4 fragColor;

float sdf(in vec2 p, in vec2 b, in vec4 r) {
    r.xy = (p.x > 0.0) ? r.xz : r.yw;
    r.x  = (p.y > 0.0) ? r.x  : r.y;
    vec2 q = abs(p) - b + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

float getCornerRadius(in vec2 p, in vec4 r) {
    vec2 pair = (p.x > 0.0) ? r.xz : r.yw;
    return (p.y > 0.0) ? pair.x : pair.y;
}

void main() {
    vec2 halfSize = vSize * 0.5;
    vec2 localPos = halfSize - (vCoord * vSize);
    float radius = getCornerRadius(localPos, vRadius);
    vec2 cornerOffset = abs(localPos) - (halfSize - vec2(radius));
    bool isCorner = cornerOffset.x > 0.0 && cornerOffset.y > 0.0;
    float dist = sdf(localPos, halfSize, vRadius);
    float edge = fwidth(dist) * 0.5;
    float alpha = isCorner ? smoothstep(edge, -edge, dist) : float(dist <= 0.0);

    if (alpha <= 0.01) discard;
    fragColor = vec4(vColor.rgb, vColor.a * alpha);
}
