package io.github.aratakileo.emogg.gui.component.esm;

import io.github.aratakileo.emogg.handler.Emoji;
import org.jetbrains.annotations.NotNull;

public class EmojiOrCategoryContent {
    private final Emoji emoji;
    private final CategoryContent categoryContent;

    private EmojiOrCategoryContent(Emoji emoji, CategoryContent categoryContent) {
        this.emoji = emoji;
        this.categoryContent = categoryContent;
    }

    public EmojiOrCategoryContent(@NotNull Emoji emoji) {
        this(emoji, null);
    }

    public EmojiOrCategoryContent(@NotNull CategoryContent categoryContent) {
        this(null, categoryContent);
    }

    public boolean isEmoji() {
        return emoji != null;
    }

    public Emoji getEmoji() {
        return emoji;
    }

    public boolean isCategoryContent() {
        return categoryContent != null;
    }

    public CategoryContent getCategoryContent() {
        return categoryContent;
    }
}
