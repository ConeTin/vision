#version 150

uniform sampler2D Sampler0;
uniform vec2 HalfPixel;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec2 uv = texCoord;
    vec3 sum = texture(Sampler0, uv).rgb * 4.0;
    sum += texture(Sampler0, uv - HalfPixel).rgb;
    sum += texture(Sampler0, uv + HalfPixel).rgb;
    sum += texture(Sampler0, uv + vec2(HalfPixel.x, -HalfPixel.y)).rgb;
    sum += texture(Sampler0, uv - vec2(HalfPixel.x, -HalfPixel.y)).rgb;
    fragColor = vec4(sum / 8.0, 1.0);
}
