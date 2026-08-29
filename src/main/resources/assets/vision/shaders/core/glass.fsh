#version 150

uniform sampler2D Sampler0;
uniform vec2 ScreenSize;
uniform vec4 ColorModulator;
uniform vec2 WinSize;
uniform float Radius;
uniform vec4 Tint;

in vec2 localUv;

out vec4 fragColor;

float sdRoundRect(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + vec2(r);
    return min(max(q.x, q.y), 0.0) + length(max(q, vec2(0.0))) - r;
}

void main() {
    vec2 px = localUv * WinSize;
    vec2 center = WinSize * 0.5;
    float d = sdRoundRect(px - center, center, Radius);

    float aaw = max(fwidth(d), 0.0008);
    float shape = 1.0 - smoothstep(-aaw, aaw, d);
    if (shape <= 0.001) {
        discard;
    }

    vec2 suv = gl_FragCoord.xy / ScreenSize;
    vec3 backdrop = texture(Sampler0, suv).rgb;
    vec3 col = mix(backdrop, Tint.rgb, Tint.a);

    float borderW = 1.5;
    float rim = 1.0 - smoothstep(0.0, borderW, -d);

    vec2 hb = center - vec2(Radius);
    vec2 pc = px - center;
    vec2 pe = abs(pc) - hb;
    vec2 sgn = sign(pc);
    vec2 n = (pe.x > 0.0 && pe.y > 0.0)
            ? normalize(max(pe, vec2(0.0))) * sgn
            : ((pe.x > pe.y) ? vec2(sgn.x, 0.0) : vec2(0.0, sgn.y));
    float topF = max(0.0, -n.y);
    float leftF = max(0.0, -n.x);
    float botF = max(0.0, n.y);
    float rightF = max(0.0, n.x);

    vec2 uv01 = px / WinSize;
    float topPos = 1.0 - smoothstep(0.55, 1.0, uv01.x);
    float leftPos = 1.0 - smoothstep(0.35, 0.70, uv01.y);
    float botPos = smoothstep(0.72, 1.0, uv01.x);
    float rightPos = smoothstep(0.68, 1.0, uv01.y);
    float brPos = max(botF * botPos, rightF * rightPos);

    float pos = max(max(topF * topPos * 0.75, leftF * leftPos), brPos * 0.6);

    float lum = dot(col, vec3(0.299, 0.587, 0.114));
    float edge = rim * clamp((0.04 + 0.26 * pos) * (1.0 + 2.5 * lum), 0.0, 1.0);
    col = mix(col, vec3(1.0), edge);

    fragColor = vec4(col, shape * ColorModulator.a);
}
