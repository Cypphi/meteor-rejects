package anticope.rejects.modules;

import anticope.rejects.MeteorRejectsAddon;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

public class SkeletonESP extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<SettingColor> skeletonColorSetting = sgGeneral.add(new ColorSetting.Builder()
            .name("players-color")
            .description("The other player's color.")
            .defaultValue(new SettingColor(255, 255, 255))
            .build()
    );

    public final Setting<Boolean> distance = sgGeneral.add(new BoolSetting.Builder()
            .name("distance-colors")
            .description("Changes the color of skeletons depending on distance.")
            .defaultValue(false)
            .build()
    );

    private final Freecam freecam;

    public SkeletonESP() {
        super(MeteorRejectsAddon.CATEGORY, "skeleton-esp", "Looks cool as fuck");
        freecam = Modules.get().get(Freecam.class);
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        MatrixStack matrixStack = event.matrices;
        float g = event.tickDelta;

        mc.world.getEntities().forEach(entity -> {
            if (!(entity instanceof PlayerEntity)) return;
            if (mc.options.getPerspective() == Perspective.FIRST_PERSON && !freecam.isActive() && mc.player == entity)
                return;
            int rotationHoldTicks = Config.get().rotationHoldTicks.get();

            Color skeletonColor = PlayerUtils.getPlayerColor((PlayerEntity) entity, skeletonColorSetting.get());
            if (distance.get()) skeletonColor = getColorFromDistance(entity, g);
            PlayerEntity playerEntity = (PlayerEntity) entity;

            Vec3d footPos = getEntityRenderPosition(playerEntity, g);
            PlayerEntityRenderer livingEntityRenderer = (PlayerEntityRenderer) (LivingEntityRenderer<?, ?, ?>) mc.getEntityRenderDispatcher().getRenderer(playerEntity);
            PlayerEntityModel playerEntityModel = (PlayerEntityModel) livingEntityRenderer.getModel();

            PlayerEntityRenderState renderState = new PlayerEntityRenderState();
            livingEntityRenderer.updateRenderState(playerEntity, renderState, g);

            if (mc.player == entity && Rotations.rotationTimer < rotationHoldTicks) {
                renderState.bodyYaw = Rotations.serverYaw;
                renderState.relativeHeadYaw = 0f;
                renderState.pitch = Rotations.serverPitch;
            }

            playerEntityModel.setAngles(renderState);

            float bodyYaw = renderState.bodyYaw;
            float pitch = renderState.pitch;

            boolean swimming = playerEntity.isInSwimmingPose();
            boolean sneaking = playerEntity.isSneaking();
            boolean flying = playerEntity.isGliding();

            ModelPart head = playerEntityModel.head;
            ModelPart leftArm = playerEntityModel.leftArm;
            ModelPart rightArm = playerEntityModel.rightArm;
            ModelPart leftLeg = playerEntityModel.leftLeg;
            ModelPart rightLeg = playerEntityModel.rightLeg;

            matrixStack.push();
            matrixStack.translate(footPos.x, footPos.y, footPos.z);
            if (swimming) matrixStack.translate(0, 0.35f, 0);

            matrixStack.multiply(new Quaternionf().setAngleAxis((bodyYaw + 180) * Math.PI / 180F, 0, -1, 0));
            if (swimming || flying)
                matrixStack.multiply(new Quaternionf().setAngleAxis((90 + pitch) * Math.PI / 180F, -1, 0, 0));
            if (swimming) matrixStack.translate(0, -0.95f, 0);

            Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();
            line(event, matrix4f, 0, sneaking ? 0.6f : 0.7f, sneaking ? 0.23f : 0, 0, sneaking ? 1.05f : 1.4f, 0, skeletonColor); // spine
            line(event, matrix4f, -0.37f, sneaking ? 1.05f : 1.35f, 0, 0.37f, sneaking ? 1.05f : 1.35f, 0, skeletonColor); // shoulders
            line(event, matrix4f, -0.15f, sneaking ? 0.6f : 0.7f, sneaking ? 0.23f : 0, 0.15f, sneaking ? 0.6f : 0.7f, sneaking ? 0.23f : 0, skeletonColor); // pelvis

            // Head
            matrixStack.push();
            matrixStack.translate(0, sneaking ? 1.05f : 1.4f, 0);
            rotate(matrixStack, head);
            matrix4f = matrixStack.peek().getPositionMatrix();
            line(event, matrix4f, 0, 0, 0, 0, 0.15f, 0, skeletonColor);
            matrixStack.pop();

            // Right Leg
            matrixStack.push();
            matrixStack.translate(0.15f, sneaking ? 0.6f : 0.7f, sneaking ? 0.23f : 0);
            rotate(matrixStack, rightLeg);
            matrix4f = matrixStack.peek().getPositionMatrix();
            line(event, matrix4f, 0, 0, 0, 0, -0.6f, 0, skeletonColor);
            matrixStack.pop();

            // Left Leg
            matrixStack.push();
            matrixStack.translate(-0.15f, sneaking ? 0.6f : 0.7f, sneaking ? 0.23f : 0);
            rotate(matrixStack, leftLeg);
            matrix4f = matrixStack.peek().getPositionMatrix();
            line(event, matrix4f, 0, 0, 0, 0, -0.6f, 0, skeletonColor);
            matrixStack.pop();

            // Right Arm
            matrixStack.push();
            matrixStack.translate(0.37f, sneaking ? 1.05f : 1.35f, 0);
            rotate(matrixStack, rightArm);
            matrix4f = matrixStack.peek().getPositionMatrix();
            line(event, matrix4f, 0, 0, 0, 0, -0.55f, 0, skeletonColor);
            matrixStack.pop();

            // Left Arm
            matrixStack.push();
            matrixStack.translate(-0.37f, sneaking ? 1.05f : 1.35f, 0);
            rotate(matrixStack, leftArm);
            matrix4f = matrixStack.peek().getPositionMatrix();
            line(event, matrix4f, 0, 0, 0, 0, -0.55f, 0, skeletonColor);
            matrixStack.pop();

            matrixStack.pop();
        });
    }

    private static void line(Render3DEvent event, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, Color color) {
        Vec3d start = transform(matrix, x1, y1, z1);
        Vec3d end = transform(matrix, x2, y2, z2);
        event.renderer.line(start.x, start.y, start.z, end.x, end.y, end.z, color);
    }

    private static Vec3d transform(Matrix4f matrix, float x, float y, float z) {
        Vector4f vec = new Vector4f(x, y, z, 1f);
        matrix.transform(vec);
        return new Vec3d(vec.x, vec.y, vec.z);
    }

    private void rotate(MatrixStack matrix, ModelPart modelPart) {
        if (modelPart.roll != 0.0F) {
            matrix.multiply(RotationAxis.POSITIVE_Z.rotation(modelPart.roll));
        }

        if (modelPart.yaw != 0.0F) {
            matrix.multiply(RotationAxis.NEGATIVE_Y.rotation(modelPart.yaw));
        }

        if (modelPart.pitch != 0.0F) {
            matrix.multiply(RotationAxis.NEGATIVE_X.rotation(modelPart.pitch));
        }
    }

    private Vec3d getEntityRenderPosition(Entity entity, float tickDelta) {
        Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();
        return entity.getLerpedPos(tickDelta).subtract(cameraPos);
    }

    private Color getColorFromDistance(Entity entity, float tickDelta) {
        double distance = mc.gameRenderer.getCamera().getCameraPos().distanceTo(entity.getLerpedPos(tickDelta));
        double percent = distance / 60;

        if (percent < 0 || percent > 1) {
            color.set(0, 255, 0, 255);
            return color;
        }

        int r, g;

        if (percent < 0.5) {
            r = 255;
            g = (int) (255 * percent / 0.5);
        } else {
            g = 255;
            r = 255 - (int) (255 * (percent - 0.5) / 0.5);
        }

        color.set(r, g, 0, 255);
        return color;
    }
}
