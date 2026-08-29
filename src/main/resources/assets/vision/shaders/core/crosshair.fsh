#version 150

uniform float Morph;
uniform float QuadSize;

in vec2 uv;

out vec4 fragColor;

float sdBox(vec2 p, vec2 b) {
    vec2 q = abs(p) - b;
    return length(max(q, vec2(0.0))) + min(max(q.x, q.y), 0.0);
}

void main() {
    vec2 p = (uv - 0.5) * QuadSize;

    float arm = 4.5;
    float th = 0.6;
    float plus = min(sdBox(p, vec2(arm, th)), sdBox(p, vec2(th, arm)));

    float ring = abs(length(p) - 3.5) - 0.85;

    float d = mix(plus, ring, smoothstep(0.0, 1.0, Morph));
    float aa = max(fwidth(d), 0.0008);
    float cov = 1.0 - smoothstep(-aa, aa, d);
    if (cov <= 0.001) {
        discard;
    }
    fragColor = vec4(vec3(cov), 1.0);
}
