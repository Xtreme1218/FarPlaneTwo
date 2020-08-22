/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2020 DaPorkchop_
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

package net.daporkchop.fp2.strategy.heightmap.client;

import lombok.NonNull;
import net.daporkchop.fp2.strategy.base.client.AbstractFarRenderTile;
import net.daporkchop.fp2.strategy.heightmap.HeightmapPos;

/**
 * @author DaPorkchop_
 */
class HeightmapRenderTile extends AbstractFarRenderTile<HeightmapPos, HeightmapRenderIndex, HeightmapRenderTile> {
    public HeightmapRenderTile(HeightmapRenderCache cache, HeightmapRenderTile parent, @NonNull HeightmapPos pos) {
        super(cache, parent, pos, pos.bounds(), 4);
    }

    @Override
    protected HeightmapRenderTile[] tileArray(int size) {
        return new HeightmapRenderTile[size];
    }

    @Override
    protected int childIndex(@NonNull HeightmapPos pos) {
        int shift = this.level - pos.level() - 1;
        return (((pos.x() >> shift) & 1) << 1) | ((pos.z() >> shift) & 1);
    }
}
