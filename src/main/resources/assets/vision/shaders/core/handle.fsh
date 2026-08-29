#version 150

uniform vec2 WinSize;
uniform float Radius;
uniform float Side;
uniform float Margin;
uniform float Alpha;
uniform float HeadU;
uniform float TailU;
uniform float Hover;
uniform float Scale;
uniform vec4 ColorModulator;

in vec2 localUv;

out vec4 fragColor;

void main() {
    vec2 localPx = localUv * (WinSize + 2.0 * Margin) - vec2(Margin);
    vec2 cc = vec2(Side > 0.0 ? WinSize.x - Radius : Radius, WinSize.y - Radius);
    vec2 L = localPx - cc;
    L.x *= Side;

    float arcR = Radius + 5.0 * Scale;
    float halfPi = 1.5707963;
    float arcLen = arcR * halfPi;

    float phi = atan(L.y, L.x);
    float dPerp;
    float s;
    if (phi > halfPi) {
        dPerp = length(L - vec2(L.x, arcR));
        s = L.x;
    } else if (phi < 0.0) {
        dPerp = length(L - vec2(arcR, L.y));
        s = arcLen - L.y;
    } else {
        dPerp = abs(length(L) - arcR);
        s = arcLen * ((halfPi - phi) / halfPi);
    }

    float sLo = min(TailU, HeadU) * arcLen;
    float sHi = max(TailU, HeadU) * arcLen;
    float along = max(max(sLo - s, s - sHi), 0.0);
    float d = sqrt(dPerp * dPerp + along * along) - 2.6 * Scale;

    float aaw = max(fwidth(d), 0.0008);
    float cov = 1.0 - smoothstep(-aaw, aaw, d);
    if (cov <= 0.001) {
        discard;
    }
    float al = mix(0.5, 1.0, Hover) * Alpha;
    fragColor = vec4(1.0, 1.0, 1.0, cov * al * ColorModulator.a);
}
