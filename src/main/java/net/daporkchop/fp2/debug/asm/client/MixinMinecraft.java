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

package net.daporkchop.fp2.debug.asm.client;

import net.daporkchop.lib.common.util.PorkUtil;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.Logger;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.PixelFormat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

/**
 * @author Barteks2x
 */
@Mixin(Minecraft.class)
public abstract class MixinMinecraft {
    @Shadow
    @Final
    private static Logger LOGGER;

    @Shadow
    private boolean fullscreen;

    @Shadow
    protected abstract void updateDisplayMode() throws LWJGLException;

    /**
     * @author Barteks2x
     */
    @Overwrite
    private void createDisplay() throws LWJGLException {
        Display.setResizable(true);
        Display.setTitle("Minecraft 1.12.2");

        try {
            Display.create((new PixelFormat()).withDepthBits(24), new ContextAttribs(3, 2, ContextAttribs.CONTEXT_COMPATIBILITY_PROFILE_BIT_ARB, 0));
        } catch (LWJGLException lwjglexception) {
            LOGGER.error("Couldn't set pixel format", lwjglexception);

            PorkUtil.sleep(1000L);

            if (this.fullscreen) {
                this.updateDisplayMode();
            }

            Display.create();
        }
    }
}
