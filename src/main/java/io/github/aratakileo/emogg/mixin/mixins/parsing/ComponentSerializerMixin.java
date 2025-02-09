package io.github.aratakileo.emogg.mixin.mixins.parsing;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import io.github.aratakileo.emogg.emoji.EmojiParser;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Type;

@Mixin(Component.Serializer.class)
public abstract class ComponentSerializerMixin {
// unsupports in 1.20.4
    @Shadow public abstract JsonElement serialize(Component component, Type type, JsonSerializationContext jsonSerializationContext);

    @Inject(
            method = "serialize(Lnet/minecraft/network/chat/Component;Ljava/lang/reflect/Type;Lcom/google/gson/JsonSerializationContext;)Lcom/google/gson/JsonElement;",
            at = @At("HEAD"),
            cancellable = true)
    private void serialize(Component component, Type type, JsonSerializationContext jsonSerializationContext, CallbackInfoReturnable<JsonElement> cir) {
        EmojiParser.mixinApplyUsingOriginal(
                component, cir,
                c -> serialize(c, type, jsonSerializationContext),
                "ComponentSerializer - "
        );
    }
}
