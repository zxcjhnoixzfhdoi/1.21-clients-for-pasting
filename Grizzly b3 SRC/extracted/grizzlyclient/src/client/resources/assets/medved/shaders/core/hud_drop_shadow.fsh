#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

float roundedBoxDistance(vec2 point, vec2 halfSize, float radius) {
    vec2 q = abs(point) - halfSize + vec2(radius);
    return min(max(q.x, q.y), 0.0) + length(max(q, vec2(0.0))) - radius;
}

void main() {
    vec2 canvasSize = vec2(
        PANEL_WIDTH + 2.0 * (SHADOW_SPREAD + SHADOW_SOFTNESS * 3.0),
        PANEL_HEIGHT + 2.0 * (SHADOW_SPREAD + SHADOW_SOFTNESS * 3.0)
    );
    vec2 point = texCoord0 * canvasSize - canvasSize * 0.5;
    vec2 halfSize = vec2(PANEL_WIDTH, PANEL_HEIGHT) * 0.5 + vec2(SHADOW_SPREAD);
    float radius = max(PANEL_RADIUS + SHADOW_SPREAD, 0.0);
    float distanceFromPanel = max(roundedBoxDistance(point, halfSize, radius), 0.0);
    float normalizedDistance = distanceFromPanel / max(SHADOW_SOFTNESS, 0.001);
    float shadowAlpha = exp(-0.5 * normalizedDistance * normalizedDistance);

    if (shadowAlpha < 0.004 || vertexColor.a == 0.0) {
        discard;
    }

    float textureAlpha = texture(Sampler0, vec2(0.5)).a;
    fragColor = vec4(vertexColor.rgb, vertexColor.a * shadowAlpha * textureAlpha) * ColorModulator;
}
