#version 330

#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec4 Color;
in float dist;

out vec4 vertexColor;
out float distOut;

void main() {
    gl_Position = ProjMat * vec4(Position, 1.0);
    gl_Position.z = 0.0;

    distOut = dist;

    vertexColor = Color;
}
