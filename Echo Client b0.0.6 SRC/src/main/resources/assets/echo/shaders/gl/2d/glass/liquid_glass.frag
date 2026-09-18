#version 330 core

in vec2 vCoord;
in vec2 vUV;
in vec2 vSize;
in vec4 vRadius;
in vec4 vTint;
in float vRefractStrength;

uniform sampler2D blurTex;

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

    vec2 normal = vec2(dFdx(dist), dFdy(dist));

    float edgeFactor = smoothstep(-24.0, 0.0, dist);
    vec2 refractOffset = normal * edgeFactor * vRefractStrength;

    float r = texture(blurTex, clamp(vUV + refractOffset * 1.00, 0.0, 1.0)).r;
    float g = texture(blurTex, clamp(vUV + refractOffset * 0.85, 0.0, 1.0)).g;
    float b = texture(blurTex, clamp(vUV + refractOffset * 0.70, 0.0, 1.0)).b;
    vec3 refracted = vec3(r, g, b);
    refracted = clamp((refracted - 0.5) * 1.38 + 0.5, 0.0, 1.0);

    vec3 glass = mix(refracted, vTint.rgb, vTint.a);

    vec2 lightDir = normalize(vec2(0.6, -1.0));
    float specDir = 0.6 + 0.4 * dot(normalize(vCoord - 0.5), lightDir);
    float rim = pow(smoothstep(-9.0, 0.0, dist), 4.0) * specDir;
    glass += rim * 0.22;

    fragColor = vec4(glass, alpha);
}
