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

package net.daporkchop.fp2.client.gl.commandbuffer;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.client.gl.object.GLBuffer;
import net.daporkchop.fp2.client.gl.object.IGLBuffer;
import net.daporkchop.fp2.client.gl.object.VertexArrayObject;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.function.Consumer;

import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.opengl.GL43.*;

/**
 * Implementation of {@link IDrawCommandBuffer} which implements indirect multidraw for indexed geometry.
 *
 * @author DaPorkchop_
 */
public class IndirectIndexedMultidrawCommandBuffer extends IndirectMultidrawCommandBuffer {
    public static final int POSITION_SIZE = 4;
    public static final int COMMAND_SIZE = 5;
    public static final int ENTRY_SIZE = POSITION_SIZE + COMMAND_SIZE;

    public static final int POSITION_SIZE_BYTES = POSITION_SIZE * INT_SIZE;
    public static final int COMMAND_SIZE_BYTES = COMMAND_SIZE * INT_SIZE;
    public static final int ENTRY_SIZE_BYTES = POSITION_SIZE_BYTES + COMMAND_SIZE_BYTES;

    @Getter
    protected long vertexCount;

    public IndirectIndexedMultidrawCommandBuffer(@NonNull Consumer<VertexArrayObject> vaoInitializer, @NonNull IGLBuffer elementArray) {
        super(ENTRY_SIZE_BYTES, vaoInitializer, elementArray);
    }

    @Override
    public IDrawCommandBuffer begin() {
        super.begin();

        this.vertexCount = 0L;
        return this;
    }

    @Override
    public void drawElements(int x, int y, int z, int level, int baseVertex, int firstIndex, int count) {
        int size = this.next();

        this.vertexCount += count * 3L;

        long baseAddr = this.addr + (long) size * ENTRY_SIZE_BYTES;
        PUnsafe.putInt(baseAddr + 0 * INT_SIZE, x);
        PUnsafe.putInt(baseAddr + 1 * INT_SIZE, y);
        PUnsafe.putInt(baseAddr + 2 * INT_SIZE, z);
        PUnsafe.putInt(baseAddr + 3 * INT_SIZE, level);

        PUnsafe.putInt(baseAddr + 4 * INT_SIZE, count); //count
        PUnsafe.putInt(baseAddr + 5 * INT_SIZE, 1); //instanceCount
        PUnsafe.putInt(baseAddr + 6 * INT_SIZE, firstIndex); //firstIndex
        PUnsafe.putInt(baseAddr + 7 * INT_SIZE, baseVertex); //baseVertex
        PUnsafe.putInt(baseAddr + 8 * INT_SIZE, size); //baseInstance
    }

    @Override
    protected void multidraw0() {
        for (int i = 0; i < this.size; i++) {
            long cmd = this.addr + (long) i * ENTRY_SIZE_BYTES + POSITION_SIZE_BYTES;

            //functionally identical to original:
            /*glDrawElementsInstancedBaseVertexBaseInstance(
                    GL_TRIANGLES,
                    PUnsafe.getInt(cmd + 0 * INT_SIZE),
                    GL_UNSIGNED_SHORT,
                    PUnsafe.getInt(cmd + 2 * INT_SIZE) * SHORT_SIZE,
                    PUnsafe.getInt(cmd + 1 * INT_SIZE),
                    PUnsafe.getInt(cmd + 3 * INT_SIZE),
                    PUnsafe.getInt(cmd + 4 * INT_SIZE));*/

            //functionally identical to original, except that the instance number passed to the shader is wrong:
            /*glDrawElementsBaseVertex(
                    GL_TRIANGLES,
                    PUnsafe.getInt(cmd + 0 * INT_SIZE),
                    GL_UNSIGNED_SHORT,
                    (long) PUnsafe.getInt(cmd + 2 * INT_SIZE) * SHORT_SIZE,
                    PUnsafe.getInt(cmd + 3 * INT_SIZE));*/

            //functionally identical to original, except that the instance number passed to the shader is wrong AND it uses the wrong vertices:
            /*glDrawElements(
                    GL_TRIANGLES,
                    PUnsafe.getInt(cmd + 0 * INT_SIZE),
                    GL_UNSIGNED_SHORT,
                    (long) PUnsafe.getInt(cmd + 2 * INT_SIZE) * SHORT_SIZE);*/

            /*glDrawRangeElements(
                    GL_TRIANGLES,
                    PUnsafe.getInt(cmd + 3 * INT_SIZE),
                    PUnsafe.getInt(cmd + 3 * INT_SIZE) + 1000,
                    PUnsafe.getInt(cmd + 0 * INT_SIZE),
                    GL_UNSIGNED_SHORT,
                    (long) PUnsafe.getInt(cmd + 2 * INT_SIZE) * SHORT_SIZE);*/

            /*glDrawRangeElementsBaseVertex(
                    GL_TRIANGLES,
                    0,
                    1000,
                    PUnsafe.getInt(cmd + 0 * INT_SIZE),
                    GL_UNSIGNED_SHORT,
                    (long) PUnsafe.getInt(cmd + 2 * INT_SIZE) * SHORT_SIZE,
                    PUnsafe.getInt(cmd + 3 * INT_SIZE));*/

            //same as above
            /*glDrawElementsInstancedBaseInstance(
                    GL_TRIANGLES,
                    PUnsafe.getInt(cmd + 0 * INT_SIZE),
                    GL_UNSIGNED_SHORT,
                    (long) PUnsafe.getInt(cmd + 2 * INT_SIZE) * SHORT_SIZE,
                    1,
                    PUnsafe.getInt(cmd + 4 * INT_SIZE));*/

            //same as above
            /*glDrawElementsBaseVertex(
                    GL_TRIANGLES,
                    PUnsafe.getInt(cmd + 0 * INT_SIZE),
                    GL_UNSIGNED_SHORT,
                    (long) PUnsafe.getInt(cmd + 2 * INT_SIZE) * SHORT_SIZE,
                    48814);*/

            glDrawArraysInstancedBaseInstance(
                    GL_TRIANGLES,
                    0,
                    6,
                    1,
                    PUnsafe.getInt(cmd + 4 * INT_SIZE));
        }
    }
}
