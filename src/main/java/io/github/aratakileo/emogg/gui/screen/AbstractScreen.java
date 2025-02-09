package io.github.aratakileo.emogg.gui.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public abstract class AbstractScreen extends Screen {
    protected final @Nullable Screen parent;

    protected AbstractScreen(@NotNull Component component, @Nullable Screen parent) {
        super(component);
        this.parent = parent;
    }

    protected AbstractScreen(@NotNull Component component) {
        super(component);
        this.parent = Minecraft.getInstance().screen;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public final void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float dt) {
// 1.20.4
//        renderBackground(guiGraphics, mouseX, mouseY, dt);

// 1.20.1
        renderBackground(guiGraphics);

        super.render(guiGraphics, mouseX, mouseY, dt);
        renderContent(guiGraphics, mouseX, mouseY, dt);
    }

    public void renderContent(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float dt) {
        guiGraphics.drawCenteredString(font, title, width / 2, 15, 0xffffff);
    }

    public int horizontalCenter() {
        return width / 2;
    }

    public int verticalCenter() {
        return height / 2;
    }
}
