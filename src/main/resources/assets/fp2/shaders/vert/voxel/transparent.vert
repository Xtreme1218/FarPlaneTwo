/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-$today.year DaPorkchop_
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * Any persons and/or organizations using this software must include the above copyright notice and this permission notice,
 * provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

//TODO: currently, this is just a copypasta of vert/heightmap/water.vert

//
//
// VERTEX ATTRIBUTES
//
//

layout(location = 2) in vec3 in_color;

layout(location = 3) in dvec3 in_pos_low;

void main() {
    //convert position to vec3 afterwards to minimize precision loss
    dvec3 pos = dvec3(in_pos_low.x, 63. - 2. / 16., in_pos_low.z);
    vec3 relativePos = vec3(pos - glState.camera.position);

    //vertex position is detail mixed
    gl_Position = cameraTransform(relativePos);

    vs_out.pos = relativePos;
    vs_out.base_pos = relativePos;

    //set fog depth
    fog_out.depth = length(relativePos);

    //copy trivial attributes
    vs_out.light = vec2(1., 0.);
    vs_out.state = 22;
    vs_out.color = in_color;
}