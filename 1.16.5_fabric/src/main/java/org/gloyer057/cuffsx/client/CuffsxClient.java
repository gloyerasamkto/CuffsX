package org.gloyer057.cuffsx.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import org.gloyer057.cuffsx.network.NetworkIds;

import java.util.Random;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class CuffsxClient implements ClientModInitializer {

    private static int handsBreakCooldown = 0;
    private static int legsBreakCooldown  = 0;
    private static boolean prevUseKey  = false;
    private static boolean prevJumping = false;
    private static final Random RNG = new Random();

    @Override
    public void onInitializeClient() {
        // Синхронизация наручников
        ClientPlayNetworking.registerGlobalReceiver(NetworkIds.CUFF_SYNC, (client, handler, buf, responseSender) -> {
            UUID uuid = buf.readUuid();
            boolean hands = buf.readBoolean();
            boolean legs  = buf.readBoolean();
            client.execute(() -> CuffClientState.update(uuid, hands, legs));
        });

        // HUD данные
        ClientPlayNetworking.registerGlobalReceiver(NetworkIds.CUFF_HUD, (client, handler, buf, responseSender) -> {
            int handsHp = buf.readInt();
            int legsHp  = buf.readInt();
            int applyProgress = buf.readInt();
            client.execute(() -> {
                CuffHudRenderer.handsHp = handsHp;
                CuffHudRenderer.legsHp  = legsHp;
                CuffHudRenderer.applyProgress = applyProgress;
            });
        });

        // HUD рендер
        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register(
            (matrixStack, tickDelta) -> CuffHudRenderer.render(matrixStack, tickDelta));

        // Тик для ломания наручников
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            UUID uuid = client.player.getUuid();

            if (handsBreakCooldown > 0) handsBreakCooldown--;
            if (legsBreakCooldown  > 0) legsBreakCooldown--;

            // ПКМ — ломает наручники на руках
            if (CuffClientState.hasHands(uuid)) {
                boolean usePressed = client.options.keyUse.isPressed();
                if (usePressed && !prevUseKey && handsBreakCooldown == 0) {
                    sendBreakPacket("HANDS");
                    handsBreakCooldown = RNG.nextInt(41);
                }
                prevUseKey = usePressed;
            } else prevUseKey = false;

            // Пробел — ломает наручники на ногах
            if (CuffClientState.hasLegs(uuid)) {
                boolean jumping = client.options.keyJump.isPressed();
                if (jumping && !prevJumping && legsBreakCooldown == 0) {
                    sendBreakPacket("LEGS");
                    legsBreakCooldown = RNG.nextInt(41);
                }
                prevJumping = jumping;
            } else prevJumping = false;
        });
    }

    public static void sendBreakPacket(String type) {
        var buf = PacketByteBufs.create();
        buf.writeString(type);
        ClientPlayNetworking.send(NetworkIds.CUFF_BREAK, buf);
    }
}
