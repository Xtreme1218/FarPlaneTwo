/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 DaPorkchop_
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

package net.daporkchop.fp2.mode.voxel.client;

import io.netty.buffer.ByteBuf;
import lombok.NonNull;
import net.daporkchop.fp2.client.gl.object.IGLBuffer;
import net.daporkchop.fp2.client.gl.object.VertexArrayObject;
import net.daporkchop.fp2.mode.common.client.BakeOutput;
import net.daporkchop.fp2.mode.common.client.strategy.AMDCompatibilityMultipassRenderStrategy;
import net.daporkchop.fp2.mode.voxel.VoxelDirectPosAccess;
import net.daporkchop.fp2.mode.voxel.VoxelPos;
import net.daporkchop.fp2.mode.voxel.VoxelTile;

import static net.daporkchop.fp2.mode.common.client.RenderConstants.*;
import static net.daporkchop.fp2.mode.voxel.client.VoxelRenderConstants.*;

/**
 * @author DaPorkchop_
 */
public class AMDCompatibilityVoxelRenderStrategy extends AMDCompatibilityMultipassRenderStrategy<VoxelPos, VoxelTile> implements IShaderBasedMultipassVoxelRenderStrategy {
    public AMDCompatibilityVoxelRenderStrategy() {
        super(VoxelBake.VOXEL_VERTEX_SIZE);
    }

    @Override
    protected void configureVertexAttributes(@NonNull IGLBuffer buffer, @NonNull VertexArrayObject vao) {
        VoxelBake.vertexAttributes(buffer, vao);
    }

    @Override
    protected void bakeVertsAndIndices(@NonNull VoxelPos pos, @NonNull VoxelTile[] srcs, @NonNull BakeOutput output, @NonNull ByteBuf verts, @NonNull ByteBuf[] indices) {
        VoxelBake.bakeForShaderDraw(pos, srcs, verts, indices);
    }

    @Override
    protected void drawTile(@NonNull CommandBuffer[] passes, long tile) {
        long pos = _tile_pos(tile);
        long renderData = _tile_renderData(tile);

        int pointer = _renderdata_pointer(renderData);
        if (pointer != 0) {
            DrawCommand[] commands = this.nodes[pointer].commands;

            for (int i = 0; i < RENDER_PASS_COUNT; i++) {
                if (commands[i] != null) {
                    passes[i].drawElements(
                            VoxelDirectPosAccess._x(pos), VoxelDirectPosAccess._y(pos), VoxelDirectPosAccess._z(pos), VoxelDirectPosAccess._level(pos),
                            commands[i]);
                }
            }
        }
    }
}
