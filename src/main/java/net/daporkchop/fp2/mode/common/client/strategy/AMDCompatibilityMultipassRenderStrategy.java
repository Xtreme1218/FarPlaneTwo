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

package net.daporkchop.fp2.mode.common.client.strategy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.client.gl.commandbuffer.AbstractDrawCommandBuffer;
import net.daporkchop.fp2.client.gl.commandbuffer.IDrawCommandBuffer;
import net.daporkchop.fp2.client.gl.object.GLBuffer;
import net.daporkchop.fp2.client.gl.object.IGLBuffer;
import net.daporkchop.fp2.client.gl.object.VertexArrayObject;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.mode.common.client.BakeOutput;
import net.daporkchop.fp2.util.alloc.Allocator;
import net.daporkchop.fp2.util.alloc.FixedSizeAllocator;
import net.daporkchop.lib.common.util.PArrays;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.daporkchop.lib.unsafe.util.AbstractReleasable;
import org.lwjgl.opengl.OpenGLException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static net.daporkchop.fp2.mode.common.client.RenderConstants.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL42.*;

/**
 * @author DaPorkchop_
 */
//this class is horribly inefficient because i honestly just want this to fucking work and really don't give a shit about making it fast - anything would be faster than
//  what it's currently like
//i really don't like this though
@RequiredArgsConstructor
public abstract class AMDCompatibilityMultipassRenderStrategy<POS extends IFarPos, T extends IFarTile> extends AbstractReleasable implements IMultipassRenderStrategy<POS, T> {
    /*
     * struct RenderData {
     *   u32 pointer;
     * };
     */

    protected static final long _RENDERDATA_POINTER_OFFSET = 0L;

    protected static final long _RENDERDATA_SIZE = _RENDERDATA_POINTER_OFFSET + INT_SIZE;

    protected static int _renderdata_pointer(long renderData) {
        return PUnsafe.getInt(renderData + _RENDERDATA_POINTER_OFFSET);
    }

    protected static void _renderdata_pointer(long renderData, int pointer) {
        PUnsafe.putInt(renderData + _RENDERDATA_POINTER_OFFSET, pointer);
    }

    protected final int vertexSize;

    protected RenderNode[] nodes;
    protected final Allocator nodeAllocator = new FixedSizeAllocator(1L, new Allocator.SequentialHeapManager() {
        @Override
        public void brk(long capacity) {
            //initially allocate array
            AMDCompatibilityMultipassRenderStrategy.this.nodes = new RenderNode[toInt(capacity, "capacity")];
        }

        @Override
        public void sbrk(long newCapacity) {
            //extend array to new capacity
            AMDCompatibilityMultipassRenderStrategy.this.nodes = Arrays.copyOf(AMDCompatibilityMultipassRenderStrategy.this.nodes, toInt(newCapacity, "newCapacity"));
        }
    });

    @Getter
    protected final CommandBuffer[] passes = PArrays.filled(RENDER_PASS_COUNT, CommandBuffer[]::new, CommandBuffer::new);

    @Override
    public long renderDataSize() {
        return _RENDERDATA_SIZE;
    }

    @Override
    public void deleteRenderData(long renderData) {
        int pointer = _renderdata_pointer(renderData);
        if (pointer >= 0) { //erase node if needed
            _renderdata_pointer(renderData, -1);
            this.nodeAllocator.free(pointer);

            if (this.nodes[pointer] == null) {
                return; //if the node is already null, the array was likely cleared in advanced by doRelease(), so we can simply ignore it and assume it's already been deleted
            }

            this.nodes[pointer].delete();
            this.nodes[pointer] = null;
        }
    }

    @Override
    public boolean bake(@NonNull POS pos, @NonNull T[] srcs, @NonNull BakeOutput output) {
        ByteBuf verts = UnpooledByteBufAllocator.DEFAULT.directBuffer();
        ByteBuf[] indices = new ByteBuf[RENDER_PASS_COUNT];
        for (int i = 0; i < RENDER_PASS_COUNT; i++) {
            indices[i] = UnpooledByteBufAllocator.DEFAULT.directBuffer();
        }

        try {
            this.bakeVertsAndIndices(pos, srcs, output, verts, indices);

            int vertexCount = verts.readableBytes() / this.vertexSize;
            if (vertexCount == 0) { //there are no vertices, meaning nothing to draw
                return false;
            } else if (Stream.of(indices).noneMatch(ByteBuf::isReadable)) { //there are no indices, meaning nothing to draw
                return false;
            }

            verts.retain(); //retain all buffers that we need to
            for (ByteBuf buf : indices) {
                if (buf.isReadable()) {
                    buf.retain();
                }
            }

            _renderdata_pointer(output.renderData, -1);

            output.submit(renderData -> {
                //create and upload vertex data
                GLBuffer vbo = new GLBuffer(GL_STATIC_DRAW);
                try (GLBuffer boundVbo = vbo.bind(GL_ARRAY_BUFFER)) {
                    boundVbo.upload(verts);
                }
                verts.release();

                RenderNode node = new RenderNode(vbo);

                for (int pass = 0; pass < RENDER_PASS_COUNT; pass++) { //create vao for every render pass that has something to be rendered
                    if (indices[pass].refCnt() == 0) { //skip empty index buffers
                        continue;
                    }

                    int indexCount = indices[pass].readableBytes() >> INDEX_SHIFT;

                    //create and upload index data
                    GLBuffer elementArray = new GLBuffer(GL_STATIC_DRAW);
                    try (GLBuffer boundElementArray = elementArray.bind(GL_ELEMENT_ARRAY_BUFFER)) {
                        boundElementArray.upload(indices[pass]);
                    }
                    indices[pass].release();

                    //create and initialize vertex array object
                    VertexArrayObject vao = new VertexArrayObject();
                    try (VertexArrayObject boundVao = vao.bindForChange()) {
                        this.passes[pass].initializeVao(boundVao);
                        this.configureVertexAttributes(vbo, boundVao);

                        boundVao.putElementArray(elementArray);
                    }

                    //create and store draw command for this tile for this render pass
                    node.commands[pass] = new DrawCommand(vao, indexCount, elementArray);
                }

                int pointer = toInt(this.nodeAllocator.alloc(1L));
                this.nodes[pointer] = node;
                _renderdata_pointer(renderData, pointer);
            });
            return true;
        } finally {
            for (ByteBuf buf : indices) {
                buf.release();
            }
            verts.release();
        }
    }

    protected abstract void bakeVertsAndIndices(@NonNull POS pos, @NonNull T[] srcs, @NonNull BakeOutput output, @NonNull ByteBuf verts, @NonNull ByteBuf[] indices);

    @Override
    public void executeBakeOutput(@NonNull POS pos, @NonNull BakeOutput output) {
        output.execute();
    }

    @Override
    public void drawTile(@NonNull IDrawCommandBuffer[] passes, long tile) {
        this.drawTile((CommandBuffer[]) passes, tile);
    }

    protected abstract void drawTile(@NonNull CommandBuffer[] passes, long tile);

    @Override
    protected void doRelease() {
        //delete all nodes that exist
        for (int i = 0; i < this.nodes.length; i++) {
            if (this.nodes[i] != null) {
                this.nodes[i].delete();
                this.nodes[i] = null;
            }
        }

        for (CommandBuffer commandBuffer : this.passes) {
            commandBuffer.delete();
        }
    }

    protected abstract void configureVertexAttributes(@NonNull IGLBuffer buffer, @NonNull VertexArrayObject vao);

    @RequiredArgsConstructor
    protected static class RenderNode {
        public final DrawCommand[] commands = new DrawCommand[RENDER_PASS_COUNT];
        @NonNull
        private final GLBuffer vbo;

        public void delete() {
            for (DrawCommand command : this.commands) {
                if (command != null) {
                    command.delete();
                }
            }
            this.vbo.delete();
        }
    }

    @RequiredArgsConstructor
    protected static class DrawCommand {
        @NonNull
        protected final VertexArrayObject vao;
        protected final int count;
        @NonNull
        protected final GLBuffer elementArray;

        public void draw(int instanceId) {
            try (VertexArrayObject vao = this.vao.bind()) {
                glDrawElementsInstancedBaseInstance(GL_TRIANGLES, this.count, GL_UNSIGNED_SHORT, 0L, 1, instanceId);
            }
        }

        public void delete() {
            this.vao.delete();
            this.elementArray.delete();
        }
    }

    protected static class CommandBuffer extends AbstractDrawCommandBuffer {
        public static final int POSITION_SIZE = 4;
        public static final int POSITION_SIZE_BYTES = POSITION_SIZE * INT_SIZE;

        protected final List<DrawCommand> commands = new ArrayList<>();

        public CommandBuffer() {
            super(POSITION_SIZE_BYTES);
        }

        @Override
        protected void upload(long addr, long size) {
            try (GLBuffer buffer = this.buffer.bind(GL_ARRAY_BUFFER)) {
                buffer.upload(addr, size);
            }
        }

        @Override
        protected void draw0() {
            for (int i = 0, lim = this.size; i < lim; i++) {
                this.commands.get(i).draw(i);
            }
        }

        public void initializeVao(@NonNull VertexArrayObject vao) {
            vao.attrI(this.buffer, 4, GL_INT, POSITION_SIZE_BYTES, 0, 1);
        }

        public void drawElements(int x, int y, int z, int level, @NonNull DrawCommand command) {
            int size = this.next();

            long baseAddr = this.addr + (long) size * POSITION_SIZE_BYTES;
            PUnsafe.putInt(baseAddr + 0 * INT_SIZE, x);
            PUnsafe.putInt(baseAddr + 1 * INT_SIZE, y);
            PUnsafe.putInt(baseAddr + 2 * INT_SIZE, z);
            PUnsafe.putInt(baseAddr + 3 * INT_SIZE, level);

            this.commands.add(command);
        }

        @Override
        @Deprecated
        public void drawElements(int x, int y, int z, int level, int baseVertex, int firstIndex, int count) {
            throw new UnsupportedOperationException();
        }

        @Override
        @Deprecated
        public long vertexCount() {
            throw new UnsupportedOperationException();
        }
    }
}
