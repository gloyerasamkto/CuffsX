package org.gloyer057.cuffsx.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;

@Environment(EnvType.CLIENT)
public class CuffHudRenderer {

    public static int handsHp = -1;
    public static int legsHp  = -1;
    public static int applyProgress = -1;

    public static void render(MatrixStack matrices, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        TextRenderer tr = client.textRenderer;
        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();

        int y = screenH - 60;
        if (handsHp >= 0) {
            String text = "Руки: " + handsHp + "/" + org.gloyer057.cuffsx.cuff.CuffDurability.MAX_HP;
            int color = hpColor(handsHp);
            tr.drawWithShadow(matrices, text, 8, y, color);
            y += 12;
        }
        if (legsHp >= 0) {
            String text = "Ноги: " + legsHp + "/" + org.gloyer057.cuffsx.cuff.CuffDurability.MAX_HP;
            int color = hpColor(legsHp);
            tr.drawWithShadow(matrices, text, 8, y, color);
        }

        if (applyProgress >= 0) {
            String bar = buildBar(applyProgress);
            String text = bar + " " + applyProgress + "%";
            int textW = tr.getWidth(text);
            tr.drawWithShadow(matrices, text, (screenW - textW) / 2, screenH / 2 + 30, 0xFFFFAA00);
        }
    }

    private static int hpColor(int hp) {
        if (hp > 60) return 0xFF55FF55;
        if (hp > 30) return 0xFFFFAA00;
        return 0xFFFF5555;
    }

    private static String buildBar(int percent) {
        int filled = percent / 10;
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 10; i++) sb.append(i < filled ? "#" : ".");
        sb.append("]");
        return sb.toString();
    }
}
