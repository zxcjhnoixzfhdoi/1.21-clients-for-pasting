#version 460

layout(location = 0) in vec4 vertex_color;
layout(location = 1) in vec3 world_pos;
layout(location = 2) in flat int face_dir;
layout(location = 3) in flat int fill;
layout(location = 4) in flat int outline;


layout(location = 0) out vec4 frag_color;

const int dir_to_axis[3] = int[](1, 0, 2);
const float line_width = 1.5;

void main() {
    vec3 f = fract(world_pos);
    vec3 d = fwidth(world_pos);
    vec3 edge = smoothstep(vec3(0.0), d * line_width, f)
            * smoothstep(vec3(0.0), d * line_width, 1.0 - f);

    int axis = dir_to_axis[face_dir / 2];
    edge[axis] = 1.0;

    bool is_outline = min(edge.x, min(edge.y, edge.z)) < 0.5;

    if (is_outline && outline != 0) {
        frag_color = vec4(vertex_color.rgb, 1.0);
    } else if (fill != 0) {
        frag_color = vertex_color;
    } else {
        discard;
    }
}