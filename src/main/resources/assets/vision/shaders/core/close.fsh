#version 150

uniform vec2 Size;
uniform float Hover;
uniform float Show;
uniform vec4 ColorModulator;

in vec2 localUv;

out vec4 fragColor;

float sdBox(vec2 p, vec2 b) {
    vec2 q = abs(p) - b;
    return length(max(q, vec2(0.0))) + min(max(q.x, q.y), 0.0);
}

void main() {
    vec2 p = (localUv - 0.5) * Size;

    float r = mix(Size.x * 0.135, Size.x * 0.346, Hover);
    float dc = length(p) - r;
    float aaw = max(fwidth(dc), 0.0008);
    float circle = 1.0 - smoothstep(-aaw, aaw, dc);
    if (circle <= 0.001) {
        discard;
    }

    vec3 col = vec3(1.0);
    vec2 q = vec2(p.x + p.y, -p.x + p.y) * 0.70710678;
    float xa = Size.x * 0.173;
    float xt = Size.x * 0.031;
    float xsd = min(sdBox(q, vec2(xa, xt)), sdBox(q, vec2(xt, xa)));
    float xMask = (1.0 - smoothstep(-aaw, aaw, xsd)) * Hover;
    col = mix(col, vec3(0.20), xMask);

    float baseA = mix(0.55, 1.0, Hover);
    fragColor = vec4(col, circle * baseA * Show * ColorModulator.a);
}
