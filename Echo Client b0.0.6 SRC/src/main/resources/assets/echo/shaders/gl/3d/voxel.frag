#version 330

in vec4 vertexColor;
in vec3 worldPos;
flat in int faceDir;
out vec4 fragColor;

uniform bool fill;
uniform bool outline;

const int dirToAxis[3] = int[](1, 0, 2);
const float lineWidth = 1.5;

void main() {
    vec3 f = fract(worldPos);
    vec3 d = fwidth(worldPos);
    vec3 edge = smoothstep(vec3(0.0), d * lineWidth, f) * smoothstep(vec3(0.0), d * lineWidth, 1.0 - f);

    int axis = dirToAxis[faceDir / 2];
    edge[axis] = 1.0;

    bool isOutline = min(edge.x, min(edge.y, edge.z)) < 0.5;

    if (isOutline && outline) {
        fragColor = vec4(vertexColor.rgb, 1.0);
    } else if (fill) {
        fragColor = vertexColor;
    } else {
        discard;
    }
}
