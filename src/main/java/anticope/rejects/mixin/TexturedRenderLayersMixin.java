package anticope.rejects.mixin;

import anticope.rejects.modules.Rendering;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.block.entity.state.ChestBlockEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(TexturedRenderLayers.class)
public class TexturedRenderLayersMixin {
    @ModifyVariable(method = "getChestTextureId(Lnet/minecraft/client/render/block/entity/state/ChestBlockEntityRenderState$Variant;Lnet/minecraft/block/enums/ChestType;)Lnet/minecraft/client/util/SpriteIdentifier;", at = @At("HEAD"), argsOnly = true)
    private static ChestBlockEntityRenderState.Variant chrsitmas(ChestBlockEntityRenderState.Variant variant) {
        Rendering rendering = Modules.get().get(Rendering.class);
        if (rendering != null && rendering.chistmas())
            return ChestBlockEntityRenderState.Variant.CHRISTMAS;
        return variant;
    }
}
