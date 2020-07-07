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

package net.daporkchop.fp2.client.height;

import net.daporkchop.fp2.client.common.TerrainRenderer;
import net.daporkchop.fp2.client.render.MatrixHelper;
import net.daporkchop.fp2.client.render.object.ElementArrayObject;
import net.daporkchop.fp2.client.render.object.VertexArrayObject;
import net.daporkchop.fp2.client.render.object.VertexBufferObject;
import net.daporkchop.fp2.client.render.shader.ShaderManager;
import net.daporkchop.fp2.client.render.shader.ShaderProgram;
import net.daporkchop.lib.common.math.BinMath;
import net.daporkchop.lib.common.util.PorkUtil;
import net.daporkchop.lib.noise.NoiseSource;
import net.daporkchop.lib.noise.engine.OpenSimplexNoiseEngine;
import net.daporkchop.lib.random.impl.FastPRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.ChunkPos;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static net.daporkchop.fp2.util.Constants.*;
import static net.minecraft.client.renderer.OpenGlHelper.GL_STATIC_DRAW;
import static net.minecraft.client.renderer.OpenGlHelper.glBindBuffer;
import static net.minecraft.client.renderer.OpenGlHelper.glGenBuffers;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * @author DaPorkchop_
 */
public class HeightTerrainRenderer extends TerrainRenderer {
    public static ShaderProgram HEIGHT_SHADER = ShaderManager.get("height");
    public static final ElementArrayObject MESH = new ElementArrayObject();
    public static final int MESH_VERTEX_COUNT;

    public static final VertexBufferObject COORDS = new VertexBufferObject();

    static {
        ShortBuffer meshData = BufferUtils.createShortBuffer(HEIGHT_TILE_VERTS * HEIGHT_TILE_VERTS * 2 + 1);
        MESH_VERTEX_COUNT = genMesh(HEIGHT_TILE_VERTS, HEIGHT_TILE_VERTS, meshData);

        try (ElementArrayObject mesh = MESH.bind()) {
            GL15.glBufferData(GL_ELEMENT_ARRAY_BUFFER, (ShortBuffer) meshData.flip(), GL_STATIC_DRAW);
        }
    }

    static {
        FloatBuffer coordsData = BufferUtils.createFloatBuffer(HEIGHT_TILE_VERTS * HEIGHT_TILE_VERTS * 2);
        for (int x = 0; x < HEIGHT_TILE_VERTS; x++) {
            for (int z = 0; z < HEIGHT_TILE_VERTS; z++) {
                coordsData.put(x).put(z);
            }
        }

        try (VertexBufferObject coords = COORDS.bind()) {
            GL15.glBufferData(GL_ARRAY_BUFFER, (FloatBuffer) coordsData.flip(), GL_STATIC_DRAW);
        }
    }

    private static int genMesh(int size, int edge, ShortBuffer out) {
        int verts = 0;
        for (int x = 0; x < size - 1; x++) {
            if ((x & 1) == 0) {
                for (int z = 0; z < size; z++, verts += 2) {
                    out.put((short) (x * edge + z)).put((short) ((x + 1) * edge + z));
                }
            } else {
                for (int z = size - 1; z > 0; z--, verts += 2) {
                    out.put((short) ((x + 1) * edge + z)).put((short) (x * edge + z - 1));
                }
            }
        }
        if ((size & 1) != 0 && size > 2) {
            out.put((short) ((size - 1) * edge));
            return verts + 1;
        } else {
            return verts;
        }
    }

    public static final NoiseSource NOISE = new OpenSimplexNoiseEngine(new FastPRandom()).scaled(0.03d).octaves(8).mul(256d);
    public static final Map<ChunkPos, VertexArrayObject> VAO_LOOKUP = Collections.synchronizedMap(new HashMap<>());

    static {
        new Thread(() -> {
            for (int x = 0; x < 5; x++) {
                for (int z = 0; z < 5; z++) {
                    PorkUtil.sleep(5000L);

                    IntBuffer buffer = BufferUtils.createIntBuffer(HEIGHT_TILE_VERTS * HEIGHT_TILE_VERTS);
                    for (int xx = 0; xx < HEIGHT_TILE_VERTS; xx++) {
                        for (int zz = 0; zz < HEIGHT_TILE_VERTS; zz++) {
                            buffer.put((int) NOISE.get(x * HEIGHT_TILE_SQUARES + xx, z * HEIGHT_TILE_SQUARES + zz));
                        }
                    }
                    buffer.flip();

                    ChunkPos pos = new ChunkPos(x, z);
                    Minecraft.getMinecraft().addScheduledTask(() -> {
                        try (VertexArrayObject vao = new VertexArrayObject().bind())    {
                            glEnableVertexAttribArray(0);
                            glEnableVertexAttribArray(1);

                            try (VertexBufferObject coords = COORDS.bind()) {
                                glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0L);
                                vao.putDependency(0, coords);
                            }
                            try (VertexBufferObject heights = new VertexBufferObject().bind())  {
                                glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
                                glVertexAttribIPointer(1, 1, GL_INT, 0, 0L);
                                vao.putDependency(1, heights);
                            }

                            vao.putElementArray(MESH.bind());

                            VAO_LOOKUP.put(pos, vao);
                        } finally {
                            glDisableVertexAttribArray(0);
                            glDisableVertexAttribArray(1);

                            MESH.close();
                        }
                    });
                }
            }
        }).start();
    }

    @Override
    public void render(float partialTicks, WorldClient world, Minecraft mc) {
        super.render(partialTicks, world, mc);

        //GlStateManager.disableFog();
        GlStateManager.disableAlpha();
        //GlStateManager.enableBlend();
        GlStateManager.disableCull();

        glPushMatrix();
        glTranslated(-this.cameraX, -this.cameraY, -this.cameraZ);

        this.modelView = MatrixHelper.getMatrix(GL_MODELVIEW_MATRIX, this.modelView);
        this.proj = MatrixHelper.getMatrix(GL_PROJECTION_MATRIX, this.proj);

        try (ShaderProgram shader = HEIGHT_SHADER.use()) {
            ARBShaderObjects.glUniformMatrix4ARB(shader.uniformLocation("camera_projection"), false, this.proj);
            ARBShaderObjects.glUniformMatrix4ARB(shader.uniformLocation("camera_modelview"), false, this.modelView);

            VAO_LOOKUP.forEach((pos, o) -> {
                ARBShaderObjects.glUniform2fARB(shader.uniformLocation("offset"), pos.x * HEIGHT_TILE_SQUARES, pos.z * HEIGHT_TILE_SQUARES);

                try (VertexArrayObject vao = o.bind())  {
                    glDrawElements(GL_TRIANGLE_STRIP, MESH_VERTEX_COUNT, GL_UNSIGNED_SHORT, 0L);
                }
            });
        }

        glPopMatrix();

        GlStateManager.enableCull();
        //GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        //GlStateManager.enableFog();
    }
}
