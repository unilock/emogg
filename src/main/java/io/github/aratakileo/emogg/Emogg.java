package io.github.aratakileo.emogg;

import io.github.aratakileo.emogg.emoji.EmojiManager;
import io.github.aratakileo.emogg.gui.EmojiSuggestion;
import io.github.aratakileo.suggestionsapi.SuggestionsAPI;
import io.github.aratakileo.suggestionsapi.injector.Injector;
import io.github.aratakileo.suggestionsapi.util.Cast;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;


public class Emogg implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(Emogg.class);

    public static final String NAMESPACE_OR_ID = "emogg";

    @Override
    public void onInitializeClient() {
        SuggestionsAPI.registerInjector(Injector.simple(
                Pattern.compile("[:：][A-Za-z0-9_]*([:：])?$"),
                (currentExpression, startOffset) -> {
                    List<EmojiSuggestion> suggestions = new LinkedList<>();
                    EmojiManager.getInstance().getEmojisStream().forEach(emoji -> {
                        suggestions.add(new EmojiSuggestion(emoji, ":%s:".formatted(emoji.getName())));
                        suggestions.add(new EmojiSuggestion(emoji, "：%s：".formatted(emoji.getName())));
                    });
                   return Cast.of(suggestions);
                }
        ));

        EmoggConfig.load();
        EmojiManager.init();

        registerBuiltinResourcePack("twemogg");
    }

    private void registerBuiltinResourcePack(@NotNull String resourcepackName) {
        ResourceManagerHelper.registerBuiltinResourcePack(
                new ResourceLocation(NAMESPACE_OR_ID, resourcepackName),
                FabricLoader.getInstance().getModContainer(NAMESPACE_OR_ID).orElseThrow(),
                Component.translatable(String.format("emogg.resourcepack.%s.name", resourcepackName)),
                ResourcePackActivationType.DEFAULT_ENABLED
        );
    }
}
