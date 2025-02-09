package io.github.aratakileo.emogg;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.datafixers.util.Pair;
import io.github.aratakileo.emogg.util.MultiUniform;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.client.renderer.RenderType.CompositeState.CompositeStateBuilder;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static net.minecraft.client.renderer.RenderStateShard.*;

@Environment(EnvType.CLIENT)
public class EmoggRenderTypes {

    // ########## Helper Function ##########

    private static RenderType setupEmojiRT(RenderType renderType) {
// 1.20.4
//        Minecraft.getInstance()
//                .renderBuffers()
//                .bufferSource()
//                .fixedBuffers
//                .put(renderType, new BufferBuilder(renderType.bufferSize()));

// 1.20.1
        Minecraft.getInstance()
                .renderBuffers()
                .fixedBuffers
                .put(renderType, new BufferBuilder(renderType.bufferSize()));
        return renderType;
    }

    private static CompositeStateBuilder beginStateWithShaderMaybeTexture(
            ShaderStateShard shader,
            @Nullable ResourceLocation texture
    ) {
        final var builder = CompositeState.builder();

        builder.setShaderState(shader);
        builder.setTextureState(texture == null ? NO_TEXTURE : new TextureStateShard(texture, true, false));
        builder.setTransparencyState(TRANSLUCENT_TRANSPARENCY);
        builder.setLightmapState(LIGHTMAP);

        return builder;
    }

    private static RenderType createRT(
            String name,
            VertexFormat.Mode vertexMode,
            ShaderStateShard shader,
            @Nullable ResourceLocation texture,
            boolean needUV
    ) {
        return setupEmojiRT(RenderType.create(
                name,
                (texture != null || needUV)
                        ? DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP
                        : DefaultVertexFormat.POSITION_COLOR_LIGHTMAP,
                vertexMode,
                256,
                false,
                true,
                beginStateWithShaderMaybeTexture(shader, texture)
                        .createCompositeState(false)
        ));
    }

    private static RenderType createRTSeeThrough(
            String name,
            VertexFormat.Mode vertexMode,
            ShaderStateShard shader,
            @Nullable ResourceLocation texture,
            boolean needUV
    ) {
        return setupEmojiRT(RenderType.create(
                name + "_see_through",
                (texture != null || needUV)
                        ? DefaultVertexFormat.POSITION_COLOR_TEX
                        : DefaultVertexFormat.POSITION_COLOR,
                vertexMode,
                256,
                false,
                true,
                beginStateWithShaderMaybeTexture(shader, texture)
                        .setDepthTestState(NO_DEPTH_TEST)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false)
        ));
    }

    private static RenderType createRTPolygonOffset(
            String name,
            VertexFormat.Mode vertexMode,
            ShaderStateShard shader,
            @Nullable ResourceLocation texture,
            boolean needUV
    ) {
        return setupEmojiRT(RenderType.create(
                name + "_polygon_offset",
                (texture != null || needUV)
                        ? DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP
                        : DefaultVertexFormat.POSITION_COLOR_LIGHTMAP,
                vertexMode,
                256,
                false,
                true,
                beginStateWithShaderMaybeTexture(shader, texture)
                        .setLayeringState(POLYGON_OFFSET_LAYERING)
                        .createCompositeState(false)
        ));
    }

    private static GlyphRenderTypes createGlyphRT(
            String name,
            VertexFormat.Mode vertexMode,
            ShaderStateShard shader,
            ShaderStateShard seeThroughShader,
            @Nullable ResourceLocation texture,
            boolean needUV
    ) {
        return new GlyphRenderTypes(
                createRT(name, vertexMode, shader, texture, needUV),
                createRTSeeThrough(name, vertexMode, seeThroughShader, texture, needUV),
                createRTPolygonOffset(name, vertexMode, shader, texture, needUV)
        );
    }

    private static boolean customShadersAvailable = false;

    private static boolean useCustomShaders() {
        return EmoggConfig.instance.enableCustomShaders && customShadersAvailable;
    }

    // ########## Render Type Definitions ##########

    private static final Function<ResourceLocation, GlyphRenderTypes> EMOJI_TEXTURED =
            Util.memoize(texture -> createGlyphRT(
                    "emoji_textured",
                    VertexFormat.Mode.QUADS,
                    Shaders.EMOJI_TEXTURED,
                    Shaders.EMOJI_TEXTURED_SEE_THROUGH,
                    texture, true
            ));
    private static final Function<ResourceLocation, GlyphRenderTypes> EMOJI_TEXTURED_VANILLA =
            Util.memoize(texture -> createGlyphRT(
                    "emoji_textured_vanilla",
                    VertexFormat.Mode.QUADS,
                    RENDERTYPE_TEXT_SHADER,
                    RENDERTYPE_TEXT_SEE_THROUGH_SHADER,
                    texture, true
            ));

    public static GlyphRenderTypes emojiTextured(ResourceLocation texture) {
        if (useCustomShaders()) {
            return EMOJI_TEXTURED.apply(texture);
        } else {
            return EMOJI_TEXTURED_VANILLA.apply(texture);
        }
    }

    private static final Function<ResourceLocation, GlyphRenderTypes> EMOJI_TEXTURED_GRAYSCALE =
            Util.memoize(texture -> createGlyphRT(
                    "emoji_textured_grayscale",
                    VertexFormat.Mode.QUADS,
                    Shaders.EMOJI_TEXTURED_GRAYSCALE,
                    Shaders.EMOJI_TEXTURED_GRAYSCALE_SEE_THROUGH,
                    texture, true
            ));

    public static GlyphRenderTypes emojiTexturedGrayscale(ResourceLocation texture) {
        if (useCustomShaders()) {
            return EMOJI_TEXTURED_GRAYSCALE.apply(texture);
        } else {
            // No vanilla fallback, falling back to colored
            return EMOJI_TEXTURED_VANILLA.apply(texture);
        }
    }

    private static final GlyphRenderTypes EMOJI_NO_TEXTURE =
            createGlyphRT(
                    "emoji_no_texture",
                    VertexFormat.Mode.TRIANGLES,
                    Shaders.EMOJI_NO_TEXTURE,
                    Shaders.EMOJI_NO_TEXTURE_SEE_THROUGH,
                    null, false
            );
    private static final GlyphRenderTypes EMOJI_NO_TEXTURE_VANILLA =
            createGlyphRT(
                    "emoji_no_texture_vanilla",
                    VertexFormat.Mode.TRIANGLES,
                    RENDERTYPE_TEXT_BACKGROUND_SHADER,
                    RENDERTYPE_TEXT_BACKGROUND_SEE_THROUGH_SHADER,
                    null, false
            );

    public static GlyphRenderTypes emojiNoTexture() {
        if (useCustomShaders()) {
            return EMOJI_NO_TEXTURE;
        } else {
            return EMOJI_NO_TEXTURE_VANILLA;
        }
    }

    public static final GlyphRenderTypes EMOJI_LOADING =
            createGlyphRT(
                    "emoji_loading",
                    VertexFormat.Mode.QUADS,
                    Shaders.EMOJI_LOADING,
                    Shaders.EMOJI_LOADING_SEE_THROUGH,
                    null, true
            );

    public static GlyphRenderTypes emojiLoading() {
        if (useCustomShaders()) {
            return EMOJI_LOADING;
        } else {
            return EMOJI_NO_TEXTURE_VANILLA;
        }
    }

    // ########## Shaders ##########

    public static class Shaders {
        private static @Nullable ShaderInstance emojiTextured;
        private static @Nullable ShaderInstance emojiTexturedSeeThrough;
        private static @Nullable ShaderInstance emojiNoTexture;
        private static @Nullable ShaderInstance emojiNoTextureSeeThrough;
        private static @Nullable ShaderInstance emojiLoading;
        private static @Nullable ShaderInstance emojiLoadingSeeThrough;

        private static final ShaderStateShard EMOJI_TEXTURED = new ShaderStateShard(() -> {
            Uniform.grayscaleModeIntensity.set(0f);
            return emojiTextured;
        });
        private static final ShaderStateShard EMOJI_TEXTURED_SEE_THROUGH = new ShaderStateShard(() -> {
            Uniform.grayscaleModeIntensity.set(0f);
            return emojiTexturedSeeThrough;
        });
        private static final ShaderStateShard EMOJI_TEXTURED_GRAYSCALE = new ShaderStateShard(() -> {
            Uniform.grayscaleModeIntensity.set(1f);
            return emojiTextured;
        });
        private static final ShaderStateShard EMOJI_TEXTURED_GRAYSCALE_SEE_THROUGH = new ShaderStateShard(() -> {
            Uniform.grayscaleModeIntensity.set(1f);
            return emojiTexturedSeeThrough;
        });
        private static final ShaderStateShard EMOJI_NO_TEXTURE = new ShaderStateShard(() -> emojiNoTexture);
        private static final ShaderStateShard EMOJI_NO_TEXTURE_SEE_THROUGH = new ShaderStateShard(() -> emojiNoTextureSeeThrough);
        private static final ShaderStateShard EMOJI_LOADING = new ShaderStateShard(() -> emojiLoading);
        private static final ShaderStateShard EMOJI_LOADING_SEE_THROUGH = new ShaderStateShard(() -> emojiLoadingSeeThrough);

        public static class Uniform {
            public static MultiUniform loadingAnimationTime = new MultiUniform(2);
            public static MultiUniform grayscaleModeIntensity = new MultiUniform(2);
        }

        private static void _loadShaders(ResourceProvider resourceProvider, List<Pair<ShaderInstance, Consumer<ShaderInstance>>> list) throws IOException {
            final var NAMESPACE = Emogg.NAMESPACE_OR_ID + ":";
            
            list.add(Pair.of(
                    new ShaderInstance(resourceProvider, NAMESPACE + "emoji_textured", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP),
                    shader -> {
                        emojiTextured = shader;
                        Uniform.grayscaleModeIntensity.uniforms[0] = shader.safeGetUniform("grayscale");
                    }
            ));
            list.add(Pair.of(
                    new ShaderInstance(resourceProvider, NAMESPACE + "emoji_textured_see_through", DefaultVertexFormat.POSITION_COLOR_TEX),
                    shader -> {
                        emojiTexturedSeeThrough = shader;
                        Uniform.grayscaleModeIntensity.uniforms[1] = shader.safeGetUniform("grayscale");
                    }
            ));
            list.add(Pair.of(
                    new ShaderInstance(resourceProvider, NAMESPACE + "emoji_no_texture", DefaultVertexFormat.POSITION_COLOR_LIGHTMAP),
                    shader -> emojiNoTexture = shader
            ));
            list.add(Pair.of(
                    new ShaderInstance(resourceProvider, NAMESPACE + "emoji_no_texture_see_through", DefaultVertexFormat.POSITION_COLOR),
                    shader -> emojiNoTextureSeeThrough = shader
            ));
            list.add(Pair.of(
                    new ShaderInstance(resourceProvider, NAMESPACE + "emoji_loading", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP),
                    shader -> {
                        emojiLoading = shader;
                        Uniform.loadingAnimationTime.uniforms[0] = shader.safeGetUniform("animationTime");
                    }
            ));
            list.add(Pair.of(
                    new ShaderInstance(resourceProvider, NAMESPACE + "emoji_loading_see_through", DefaultVertexFormat.POSITION_COLOR_TEX),
                    shader -> {
                        emojiLoadingSeeThrough = shader;
                        Uniform.loadingAnimationTime.uniforms[1] = shader.safeGetUniform("animationTime");
                    }
            ));
        }

        public static void loadShaders(ResourceProvider resourceProvider, List<Pair<ShaderInstance, Consumer<ShaderInstance>>> list) {
            customShadersAvailable = false;
            if (EmoggConfig.instance.enableDebugMode)
                Emogg.LOGGER.info("Loading emogg shaders...");
            
            try {
                _loadShaders(resourceProvider, list);
                customShadersAvailable = true;
            } catch (Exception e) {
                Emogg.LOGGER.error("Emogg shaders loading failed!", e);
            }
        }
    }
}
