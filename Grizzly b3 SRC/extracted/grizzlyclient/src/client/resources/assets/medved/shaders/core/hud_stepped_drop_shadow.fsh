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

float decodeRowWidth(int row) {
    vec4 encoded = texelFetch(Sampler0, ivec2(row, 0), 0);
    float lowByte = floor(encoded.r * 255.0 + 0.5);
    float highByte = floor(encoded.g * 255.0 + 0.5);
    return lowByte + highByte * 256.0;
}

void main() {
    float extent = SHADOW_SPREAD + SHADOW_SOFTNESS * 3.0;
    vec2 canvasSize = vec2(PANEL_WIDTH, PANEL_HEIGHT) + vec2(extent * 2.0);
    vec2 point = texCoord0 * canvasSize - vec2(extent);
    float unionDistance = 1000000.0;

    for (int row = 0; row < ROW_COUNT; ++row) {
        float rowWidth = decodeRowWidth(row);
        float left = ALIGN_RIGHT == 1 ? PANEL_WIDTH - rowWidth : 0.0;
        float topExtension = row > 0 ? PANEL_RADIUS : 0.0;
        float bottomExtension = row + 1 < ROW_COUNT ? PANEL_RADIUS : 0.0;
        float top = float(row) * ROW_HEIGHT - topExtension;
        float bottom = float(row + 1) * ROW_HEIGHT + bottomExtension;
        vec2 halfSize = vec2(rowWidth * 0.5, (bottom - top) * 0.5) + vec2(SHADOW_SPREAD);
        vec2 center = vec2(left + rowWidth * 0.5, (top + bottom) * 0.5);
        float radius = min(
            PANEL_RADIUS + SHADOW_SPREAD,
            min(halfSize.x, halfSize.y)
        );
        unionDistance = min(
            unionDistance,
            roundedBoxDistance(point - center, halfSize, radius)
        );
    }

    float distanceFromPanel = max(unionDistance, 0.0);
    float normalizedDistance = distanceFromPanel / max(SHADOW_SOFTNESS, 0.001);
    float shadowAlpha = exp(-0.5 * normalizedDistance * normalizedDistance);

    if (shadowAlpha < 0.004 || vertexColor.a == 0.0) {
        discard;
    }

    fragColor = vec4(vertexColor.rgb, vertexColor.a * shadowAlpha) * ColorModulator;
}
