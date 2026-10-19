package com.cbx.vibeauto.client.screen;

import com.cbx.vibeauto.network.UpdateScriptPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class ComputerScreen extends Screen {
    private final BlockPos pos;
    private final String initialCode;
    private final boolean isRunning;
    private final String status;
    private final int initialInterval;

    private MultiLineEditBox codeEditor;
    private EditBox intervalBox;
    private Button runBtn;
    private Button stopBtn;

    public ComputerScreen(BlockPos pos, String initialCode, boolean isRunning, String status, int initialInterval) {
        super(Component.literal("Computer Terminal"));
        this.pos = pos;
        this.initialCode = initialCode;
        this.isRunning = isRunning;
        this.status = status;
        this.initialInterval = initialInterval;
    }

    @Override
    protected void init() {
        int editorWidth = this.width - 40;
        int editorHeight = this.height - 90;

        codeEditor = new MultiLineEditBox(this.font, 20, 30, editorWidth, editorHeight,
                Component.literal("Code"), Component.empty());
        codeEditor.setValue(initialCode);
        this.addRenderableWidget(codeEditor);

        int bottomY = this.height - 45;

        // Run button
        runBtn = Button.builder(Component.literal("▶ Run"), btn -> {
            sendUpdate(true);
            this.onClose();
        }).bounds(20, bottomY, 70, 20).build();
        this.addRenderableWidget(runBtn);

        // Stop button
        stopBtn = Button.builder(Component.literal("■ Stop"), btn -> {
            sendUpdate(false);
            this.onClose();
        }).bounds(95, bottomY, 70, 20).build();
        this.addRenderableWidget(stopBtn);

        // Interval label & edit box
        intervalBox = new EditBox(this.font, 250, bottomY, 50, 20, Component.literal("Interval"));
        intervalBox.setValue(String.valueOf(initialInterval));
        this.addRenderableWidget(intervalBox);
    }

    private void sendUpdate(boolean run) {
        int interval = 20;
        try {
            interval = Integer.parseInt(intervalBox.getValue().trim());
        } catch (NumberFormatException ignored) {}

        PacketDistributor.sendToServer(new UpdateScriptPayload(pos, codeEditor.getValue(), run, interval));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawString(this.font, "CMP Script Editor (Pos: " + pos.toShortString() + ")", 20, 15, 0x55FFFF);
        guiGraphics.drawString(this.font, "Status: " + status, this.width - 160, 15, isRunning ? 0x55FF55 : 0xFFAA00);
        guiGraphics.drawString(this.font, "Loop Interval (ticks):", 170, this.height - 39, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
