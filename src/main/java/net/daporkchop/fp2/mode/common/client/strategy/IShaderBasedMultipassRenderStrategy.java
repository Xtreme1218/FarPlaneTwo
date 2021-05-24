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

import lombok.NonNull;
import net.daporkchop.fp2.client.DrawMode;
import net.daporkchop.fp2.client.gl.commandbuffer.IDrawCommandBuffer;
import net.daporkchop.fp2.client.gl.shader.ShaderProgram;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.BlockRenderLayer;

import static net.daporkchop.fp2.client.ClientConstants.*;

/**
 * @author DaPorkchop_
 */
public interface IShaderBasedMultipassRenderStrategy<POS extends IFarPos, T extends IFarTile> extends IMultipassRenderStrategy<POS, T> {
    @Override
    default void renderSolid(@NonNull IDrawCommandBuffer draw) {
        try (ShaderProgram program = this.blockShader().use()) {
            IMultipassRenderStrategy.super.renderSolid(draw);
        }
    }

    @Override
    default void renderCutout(@NonNull IDrawCommandBuffer draw) {
        try (ShaderProgram program = this.blockShader().use()) {
            IMultipassRenderStrategy.super.renderCutout(draw);
        }
    }

    @Override
    default void renderTransparentStencilPass(@NonNull IDrawCommandBuffer draw) {
        try (ShaderProgram program = this.stencilShader().use()) {
            IMultipassRenderStrategy.super.renderTransparentStencilPass(draw);
        }
    }

    @Override
    default void renderTransparentFragmentPass(@NonNull IDrawCommandBuffer draw) {
        try (ShaderProgram program = this.blockShaderTransparent().use()) {
            IMultipassRenderStrategy.super.renderTransparentFragmentPass(draw);
        }
    }

    /**
     * @return the shader used for rendering blocks
     */
    ShaderProgram blockShader();

    /**
     * @return the shader used for rendering blocks
     */
    default ShaderProgram blockShaderTransparent() {
        return this.blockShader();
    }

    /**
     * @return the shader used for preparing the stencil buffer
     */
    ShaderProgram stencilShader();

    @Override
    default void render(@NonNull BlockRenderLayer layer, boolean pre) {
        if (layer == BlockRenderLayer.CUTOUT && !pre) {
            ((AbstractTexture) mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE)).setBlurMipmapDirect(false, mc.gameSettings.mipmapLevels > 0);

            try (DrawMode mode = DrawMode.SHADER.begin()) {
                this.renderSolid(this.passes()[0]);
                this.renderCutout(this.passes()[1]);
                this.renderTransparent(this.passes()[2]);
            }

            mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();
        }
    }
}
