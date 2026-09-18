#version 450
#extension GL_EXT_nonuniform_qualifier : require

layout(set = 0, binding = 1) uniform sampler2D textures[];

layout(push_constant) uniform PushConstants {
    float pxRange;
} pc;

layout(location = 0) in vec2 vUV;
layout(location = 1) in vec4 vColor;
layout(location = 2) flat in int vTexIndex;

layout(location = 0) out vec4 fragColor;

float median(float r, float g, float b) {
    return max(min(r, g), min(max(r, g), b));
}

float screenPxRange() {
    vec2 unitRange = vec2(pc.pxRange) / vec2(textureSize(textures[nonuniformEXT(vTexIndex)], 0));
    vec2 screenTexSize = vec2(1.0) / fwidth(vUV);
    return max(0.5 * dot(unitRange, screenTexSize), 1.0);
}

void main() {
    vec4 msd = texture(textures[nonuniformEXT(vTexIndex)], vUV);
    float sd = median(msd.r, msd.g, msd.b);
    float screenPxDistance = screenPxRange() * (sd - 0.5);
    float alpha = clamp(screenPxDistance + 0.5, 0.0, 1.0);

    if (alpha <= 0.01) discard;
    fragColor = vec4(vColor.rgb, vColor.a * alpha);
}
