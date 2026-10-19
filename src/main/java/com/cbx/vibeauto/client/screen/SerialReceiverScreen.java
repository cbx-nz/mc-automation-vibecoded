package com.cbx.vibeauto.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class SerialReceiverScreen extends Screen {
    private final BlockPos pos;
    private final String logText;
    private MultiLineEditBox logBox;

    public SerialReceiverScreen(BlockPos pos, String logText) {
        super(Component.literal("Serial Receiver"));
        this.pos = pos;
        this.logText = logText;
    }

    @Override
    protected void init() {
        int boxWidth = this.width - 40;
        int boxHeight = this.height - 70;

        logBox = new MultiLineEditBox(this.font, 20, 30, boxWidth, boxHeight,
                Component.literal("Serial Log"), Component.empty());
        logBox.setValue(logText);
        this.addRenderableWidget(logBox);

        this.addRenderableWidget(Button.builder(Component.literal("Close"), btn -> this.onClose())
                .bounds(this.width / 2 - 40, this.height - 30, 80, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawString(this.font, "SERIAL RECEIVER TERMINAL (Baud: 9600)", 20, 15, 0x55FF55);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
