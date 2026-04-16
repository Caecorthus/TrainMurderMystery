package dev.doctor4t.wathe.mixin.client;

import com.mojang.serialization.Codec;
import dev.doctor4t.wathe.WatheConfig;
import eu.midnightdust.lib.config.MidnightConfig;
import net.minecraft.client.gui.screen.option.ControlsOptionsScreen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ControlsOptionsScreen.class)
public abstract class ControlsOptionsScreenMixin extends GameOptionsScreen {
    protected ControlsOptionsScreenMixin() {
        super(null, null, Text.empty());
    }

    @Inject(method = "addOptions", at = @At("TAIL"))
    private void wathe$addInstinctModeOption(CallbackInfo ci) {
        if (this.body != null) {
            this.body.addSingleOptionEntry(wathe$instinctModeOption());
        }
    }

    @Unique
    private static SimpleOption<WatheConfig.InstinctModeConfig> wathe$instinctModeOption() {
        return new SimpleOption<>(
                "options.wathe.instinct_mode",
                SimpleOption.emptyTooltip(),
                (optionText, value) -> Text.translatable("options.wathe.instinct_mode." + value.name().toLowerCase(java.util.Locale.ROOT)),
                new SimpleOption.PotentialValuesBasedCallbacks<>(
                        List.of(WatheConfig.InstinctModeConfig.HOLD, WatheConfig.InstinctModeConfig.TOGGLE),
                        Codec.STRING.xmap(WatheConfig.InstinctModeConfig::valueOf, WatheConfig.InstinctModeConfig::name)
                ),
                WatheConfig.instinctMode,
                value -> {
                    WatheConfig.instinctMode = value;
                    MidnightConfig.write("wathe");
                }
        );
    }
}
