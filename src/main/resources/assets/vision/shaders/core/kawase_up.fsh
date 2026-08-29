#version 150

uniform sampler2D Sampler0;
uniform vec2 HalfPixel;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec2 uv = texCoord;
    vec3 sum = texture(Sampler0, uv + vec2(-HalfPixel.x * 2.0, 0.0)).rgb;
    sum += texture(Sampler0, uv + vec2(-HalfPixel.x, HalfPixel.y)).rgb * 2.0;
    sum += texture(Sampler0, uv + vec2(0.0, HalfPixel.y * 2.0)).rgb;
    sum += texture(Sampler0, uv + vec2(HalfPixel.x, HalfPixel.y)).rgb * 2.0;
    sum += texture(Sampler0, uv + vec2(HalfPixel.x * 2.0, 0.0)).rgb;
    sum += texture(Sampler0, uv + vec2(HalfPixel.x, -HalfPixel.y)).rgb * 2.0;
    sum += texture(Sampler0, uv + vec2(0.0, -HalfPixel.y * 2.0)).rgb;
    sum += texture(Sampler0, uv + vec2(-HalfPixel.x, -HalfPixel.y)).rgb * 2.0;
    fragColor = vec4(sum / 12.0, 1.0);
}
