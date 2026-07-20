#version 330

in vec4 vertexColor;
in float dist;

out vec4 fragColor;

void main() {
//    fragColor = vec4(vec3(sin(dist)), 1.0);
    fragColor = vertexColor;
}
