#version 150

uniform vec2 BarSize;
uniform float Hover;
uniform vec4 ColorModulator;

in vec2 localUv;

out vec4 fragColor;

float sdRoundRect(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + vec2(r);
    return min(max(q.x, q.y), 0.0) + length(max(q, vec2(0.0))) - r;
}

void main() {
    vec2 px = localUv * BarSize;
    vec2 c = BarSize * 0.5;
    float r = min(BarSize.x, BarSize.y) * 0.5;
    float d = sdRoundRect(px - c, c, r);

    float aa = max(fwidth(d), 0.0008);
    float shape = 1.0 - smoothstep(-aa, aa, d);
    if (shape <= 0.001) {
        discard;
    }
    float a = mix(0.35, 1.0, Hover);
    fragColor = vec4(1.0, 1.0, 1.0, a * shape * ColorModulator.a);
}
