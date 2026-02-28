package dev.doctor4t.wathe.client.model.item;

import dev.doctor4t.wathe.item.KnifeItem;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;

public class KnifeModelLoadingPlugin implements ModelLoadingPlugin {

    public static final ModelIdentifier KNIFE_MODEL_ID = ModelIdentifier.ofInventoryVariant(KnifeItem.ITEM_ID);

    public static Identifier getModelLocation(Variant variant) {
        var variantPart = variant == Variant.DEFAULT ? "" : "_%s".formatted(variant.asString());

        return KNIFE_MODEL_ID.id().withPath(path -> "item/%s%s".formatted(path, variantPart));
    }

    @Override
    public void onInitializeModelLoader(Context pluginContext) {
        // make sure all models get loaded
        for (Variant variant : Variant.values()) {
            pluginContext.addModels(getModelLocation(variant));
        }

        pluginContext.modifyModelOnLoad().register((unbakedModel, context) -> {
            // replace the original knife model with our custom one
            if(KNIFE_MODEL_ID.equals(context.topLevelId())) {
                return new KnifeModel(unbakedModel);
            }

            return unbakedModel;
        });
    }

    public enum Variant implements StringIdentifiable {
        DEFAULT("default"),
        IN_HAND("in_hand");

        private final String name;

        Variant(String name) {
            this.name = name;
        }

        @Override
        public String asString() {
            return name;
        }
    }
}
