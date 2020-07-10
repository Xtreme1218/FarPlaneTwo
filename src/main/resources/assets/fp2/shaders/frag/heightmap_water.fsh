in vec3 vert_pos;
in flat vec4 vert_color;

layout(binding = 0) uniform sampler2D terrain_texture;

out vec4 color;

void main() {
    if (isLoaded(ivec3(vert_pos) >> 4)) {
        discard;//TODO: figure out the potential performance implications of this vs transparent output
        //color = vec4(0.);
    } else {
        TextureUV uvs = tex_uvs[9];
        //color = vert_color * texture(terrain_texture, uvs.min + (uvs.max - uvs.min) * fract(vert_pos.xz));
        color = vert_color * texture(terrain_texture, uvs.min + (uvs.max - uvs.min) * fract(vert_pos.xz));
    }
}
