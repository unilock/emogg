package pextystudios.emogg.handler;

import com.google.common.collect.Lists;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import pextystudios.emogg.Emogg;
import pextystudios.emogg.emoji.resource.Emoji;
import pextystudios.emogg.util.StringUtil;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class EmojiHandler {
    private static final Predicate<Emoji> IS_NOT_BUILTIN_EMOJI = emoji -> {
        return ConfigHandler.data.useBuiltinEmojiEnabled || !getInstance().builtinEmojis.containsKey(emoji.getName());
    };

    private static EmojiHandler INSTANCE;

    public static final String STATIC_EMOJI_EXTENSION = ".png";
    public static final String ANIMATED_EMOJI_EXTENSION = ".gif";
    public static final Predicate<String> HAS_EMOJIS_EXTENSION = path -> {
        return path.endsWith(STATIC_EMOJI_EXTENSION) || path.endsWith(ANIMATED_EMOJI_EXTENSION);
    };
    public static final String EMOJIS_PATH_PREFIX = "emoji";
    public static final int EMOJI_DEFAULT_RENDER_SIZE = 10;

    private final ConcurrentHashMap<String, Emoji> allEmojis = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Emoji> builtinEmojis = new ConcurrentHashMap<>();

    public EmojiHandler() {
        INSTANCE = this;

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(
            new SimpleSynchronousResourceReloadListener() {
                @Override
                public ResourceLocation getFabricId() {
                    return new ResourceLocation(Emogg.NAMESPACE, EMOJIS_PATH_PREFIX);
                }

                @Override
                public void onResourceManagerReload(ResourceManager resourceManager) {
                    load(resourceManager);
                }
            }
        );
    }

    public boolean hasEmoji(String name) {
        return allEmojis.containsKey(name);
    }

    public Emoji getEmoji(String name) {
        return allEmojis.get(name);
    }

    public Collection<Emoji> getEmojis() {
        return allEmojis.values().stream().filter(IS_NOT_BUILTIN_EMOJI).toList();
    }

    public Optional<Emoji> getRandomEmoji() {
        return getRandomEmoji(false);
    }

    public Optional<Emoji> getRandomEmoji(boolean includeBuiltinEmojisIfThereIsNoUserEmojis) {
        return allEmojis.values()
                .stream()
                .filter(emoji -> (
                        builtinEmojis.size() == allEmojis.size() && includeBuiltinEmojisIfThereIsNoUserEmojis
                ) || IS_NOT_BUILTIN_EMOJI.test(emoji))
                .skip(
                        (int) (
                                (
                                        allEmojis.size() - (
                                                ConfigHandler.data.useBuiltinEmojiEnabled ? 0 : builtinEmojis.size()
                                        )
                                ) * Math.random()
                        )
                )
                .findFirst();
    }

    public void regEmoji(ResourceLocation resourceLocation) {
        var emojiName = Emoji.normalizeName(Emoji.getNameFromPath(resourceLocation));
        regEmoji(resourceLocation, emojiName);
    }

    public void regEmoji(ResourceLocation resourceLocation, String emojiName) {
        if (allEmojis.containsKey(emojiName)) {
            if (allEmojis.get(emojiName).getResourceLocation().equals(resourceLocation)) {
                Emogg.LOGGER.error(String.format(
                        "Failed to load %s, because it is already defined",
                        StringUtil.repr(resourceLocation)
                ));
                return;
            }

            var emojiNameIndex = 0;
            var newEmojiName = emojiName + emojiNameIndex;

            while (allEmojis.containsKey(newEmojiName)) {
                emojiNameIndex++;
                newEmojiName = emojiName + emojiNameIndex;
            }
        }

        var emoji = Emoji.from(emojiName, resourceLocation);

        if (!emoji.isValid()) {
            Emogg.LOGGER.error(String.format(
                    "Failed to load %s, because it has invalid format",
                    StringUtil.repr(resourceLocation)
            ));
            return;
        }

        allEmojis.put(emojiName, emoji);

        Emogg.LOGGER.info(String.format("Loaded %s as %s", StringUtil.repr(resourceLocation), emoji.getCode()));
    }

    public Collection<String> getEmojiSuggestions() {
        return Lists.newArrayList(this.allEmojis.values())
                .stream()
                .filter(IS_NOT_BUILTIN_EMOJI)
                .map(Emoji::getCode)
                .collect(Collectors.toList());
    }

    private void load(ResourceManager resourceManager) {
        Emogg.LOGGER.info("Updating emoji lists...");

        if (builtinEmojis.isEmpty()) {
            Emoji emoji;

            // Start of autogenerated code
            // The code is autogenerated by generate_builtin_emojis_config.py

            emoji = Emoji.from(new ResourceLocation(Emogg.NAMESPACE,"emoji/cutie.png"));
            builtinEmojis.put(emoji.getName(), emoji);
            emoji = Emoji.from(new ResourceLocation(Emogg.NAMESPACE,"emoji/huh.png"));
            builtinEmojis.put(emoji.getName(), emoji);
            emoji = Emoji.from(new ResourceLocation(Emogg.NAMESPACE,"emoji/minecraft.gif"));
            builtinEmojis.put(emoji.getName(), emoji);
            emoji = Emoji.from(new ResourceLocation(Emogg.NAMESPACE,"emoji/stupid_exited.png"));
            builtinEmojis.put(emoji.getName(), emoji);
            emoji = Emoji.from(new ResourceLocation(Emogg.NAMESPACE,"emoji/waving_hand.gif"));
            builtinEmojis.put(emoji.getName(), emoji);

            // End of auto generated code
        }

        allEmojis.clear();
        allEmojis.putAll(builtinEmojis);

        for (var resourceLocation: resourceManager.listResources(EMOJIS_PATH_PREFIX, HAS_EMOJIS_EXTENSION)) {
            var emojiName = Emoji.normalizeName(Emoji.getNameFromPath(resourceLocation));

            if (builtinEmojis.containsKey(emojiName))
                continue;

            regEmoji(resourceLocation, emojiName);
        }

        Emogg.LOGGER.info("Updating the lists is complete!");
    }

    public static EmojiHandler getInstance() {
        return INSTANCE;
    }
}
