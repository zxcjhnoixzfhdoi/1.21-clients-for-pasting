#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

uniform sampler2D Sampler0;

in vec4 vertexColor;

out vec4 fragColor;

void main() {
    if (vertexColor.a == 0.0) {
        discard;
    }

    vec2 textureSizePx = vec2(textureSize(Sampler0, 0));
    vec2 uv = gl_FragCoord.xy / textureSizePx;

    // Tap count rises with the radius so a wide blur keeps sampling densely instead of
    // spreading a fixed 5x5 grid until it bands.
    vec2 sampleStep = vec2(BLUR_RADIUS / float(BLUR_TAPS)) / textureSizePx;
    float sigma = float(BLUR_TAPS) * 0.5;
    float twoSigmaSquared = 2.0 * sigma * sigma;

    vec3 blurred = vec3(0.0);
    float total = 0.0;

    for (int y = -BLUR_TAPS; y <= BLUR_TAPS; ++y) {
        for (int x = -BLUR_TAPS; x <= BLUR_TAPS; ++x) {
            float weight = exp(-float(x * x + y * y) / twoSigmaSquared);
            blurred += texture(Sampler0, uv + vec2(x, y) * sampleStep).rgb * weight;
            total += weight;
        }
    }

    blurred /= total;

    vec3 tinted = mix(blurred, vertexColor.rgb, TINT_ALPHA);
    fragColor = vec4(tinted, vertexColor.a) * ColorModulator;
}
