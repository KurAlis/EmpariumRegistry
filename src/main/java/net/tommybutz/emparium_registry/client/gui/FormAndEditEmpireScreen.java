package net.tommybutz.emparium_registry.client.gui;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.tommybutz.emparium_registry.EmpariumRegistry;
import net.tommybutz.emparium_registry.data.EmpireData;
import net.tommybutz.emparium_registry.data.EmpireData.FlagRatio;
import net.tommybutz.emparium_registry.network.*;
import net.tommybutz.emparium_registry.util.AspectFit;
import net.tommybutz.emparium_registry.util.PlaytimeUtil;

import javax.annotation.Nullable;
import java.text.SimpleDateFormat;
import java.util.*;

public class FormAndEditEmpireScreen extends Screen {

    private static final ResourceLocation BACKGROUND_TEXTURE = ResourceLocation
            .fromNamespaceAndPath(EmpariumRegistry.MODID, "textures/gui/form_and_edit_empire_screen.png");

    private static final int GUI_WIDTH = 400;
    private static final int GUI_HEIGHT = 330;
    private static final int GUI_TEXTURE_WIDTH = 400;
    private static final int GUI_TEXTURE_HEIGHT = 330;

    // — Pixel regions, relative to guiLeft/guiTop —
    private static final int FLAG_BTN_X1 = 25, FLAG_BTN_Y1 = 44, FLAG_BTN_X2 = 124, FLAG_BTN_Y2 = 95;
    private static final int MODEL_X1 = 77, MODEL_Y1 = 127;
    private static final int MODEL_X2 = 123, MODEL_Y2 = 203;
    private static final int MEMBERS_X1 = 25, MEMBERS_Y1 = 115, MEMBERS_X2 = 59, MEMBERS_Y2 = 203;
    private static final int DESC_X1 = 181, DESC_Y1 = 43;
    private static final int DESC_X2 = 273, DESC_Y2 = 126;
    private static final int DESC_BOX_Y2 = DESC_Y1 + 14;
    private static final int DESC_TEXT_X1 = 181, DESC_TEXT_Y1 = 43;
    private static final int CAPITAL_TEXT_X1 = 182, CAPITAL_TEXT_Y1 = 140;
    private static final int COLONIES_TEXT_X1 = 182, COLONIES_TEXT_Y1 = 157;
    private static final int CAPITAL_X1 = 182, CAPITAL_Y1 = 140;
    private static final int CAPITAL_X2 = 274, CAPITAL_Y2 = 153;
    private static final int COLONIES_X1 = 182, COLONIES_Y1 = 157;
    private static final int COLONIES_X2 = 274, COLONIES_Y2 = 171;
    private static final int NAME_X1 = 25, NAME_Y1 = 219;
    private static final int NAME_X2 = 71, NAME_Y2 = 238;
    private static final int IDEOLOGY_X1 = 78, IDEOLOGY_Y1 = 219;
    private static final int IDEOLOGY_X2 = 125, IDEOLOGY_Y2 = 257;
    private static final int IDEOLOGY_BOX_Y2 = IDEOLOGY_Y1 + 14;
    private static final int PUBLIC_BTN_X1 = 325, PUBLIC_BTN_Y1 = 8, PUBLIC_BTN_X2 = 376, PUBLIC_BTN_Y2 = 21;
    private static final int REQUESTS_BTN_X1 = 315, REQUESTS_BTN_Y1 = 106, REQUESTS_BTN_X2 = 366, REQUESTS_BTN_Y2 = 119;
    private static final Map<FlagRatio, int[]> RATIO_BUTTON_BOUNDS = Map.of(
            FlagRatio.SIXTEEN_NINE, new int[]{232, 8, 259, 21},
            FlagRatio.ONE_ONE, new int[]{263, 8, 290, 21},
            FlagRatio.TWO_ONE, new int[]{294, 8, 321, 21}
    );
    private static final int REQUESTS_PANEL_WIDTH = 120;
    private static final int REQUESTS_ROW_HEIGHT = 22;
    private static final int MAX_FLAG_DIMENSION = 512;
    private static final int INFO_X1 = 25, INFO_Y1 = 242, INFO_X2 = 71, INFO_Y2 = 257;
    private static final int MEMBER_SLOT_COUNT = 5;
    private static final float INLINE_TEXT_SCALE = 0.6f;
    private static final int INLINE_LINE_HEIGHT = 9;

    private final @Nullable EmpireData existingEmpire;
    private final boolean adminMode;
    private boolean editable;

    private int guiLeft, guiTop;

    private EditBox nameBox;
    private EditBox ideologyBox;
    private EditBox descriptionBox;
    private EditBox capitalBox;
    private EditBox coloniesBox;
    private EmpireData liveEmpire;
    private boolean isPublicSelection = true;
    private FlagRatio flagRatioSelection = FlagRatio.SIXTEEN_NINE;
    private boolean showingRequests = false;
    private final List<UUID> localPendingUUIDs = new ArrayList<>();
    private final List<String> localPendingNames = new ArrayList<>();
    private final List<Long> localPendingTimestamps = new ArrayList<>();
    private final List<net.minecraft.client.gui.components.AbstractWidget> requestRowWidgets = new ArrayList<>();

    private boolean confirmingDelete = false;

    public FormAndEditEmpireScreen(@Nullable EmpireData existingEmpire, boolean adminMode) {
        super(Component.translatable("screen.emparium_registry.form_edit_empire"));
        this.existingEmpire = existingEmpire;
        this.adminMode = adminMode;
    }

    @Override
    protected void init() {
        editable = existingEmpire == null
                || adminMode
                || (this.minecraft.player != null && existingEmpire.isEmperor(this.minecraft.player.getUUID()));

        liveEmpire = existingEmpire;
        isPublicSelection = existingEmpire == null || existingEmpire.isPublic();
        flagRatioSelection = existingEmpire == null ? FlagRatio.SIXTEEN_NINE : existingEmpire.getFlagRatio();

        guiLeft = (this.width - GUI_WIDTH) / 2;
        guiTop = (this.height - GUI_HEIGHT) / 2;

        nameBox = makeEditBox(NAME_X1, NAME_Y1, NAME_X2, NAME_Y2, 32, "Name");
        ideologyBox = makeEditBox(IDEOLOGY_X1, IDEOLOGY_Y1, IDEOLOGY_X2, IDEOLOGY_BOX_Y2, 32, "Ideology");
        descriptionBox = makeEditBox(DESC_X1, DESC_Y1, DESC_X2, DESC_BOX_Y2, 256, "Description");
        capitalBox = makeEditBox(CAPITAL_X1, CAPITAL_Y1, CAPITAL_X2, CAPITAL_Y2, 32, "Capital name");
        coloniesBox = makeEditBox(COLONIES_X1, COLONIES_Y1, COLONIES_X2, COLONIES_Y2, 128, "Colonies (semicolon-separated)");

        if (existingEmpire != null) {
            nameBox.setValue(existingEmpire.getName());
            ideologyBox.setValue(existingEmpire.getIdeology());
            descriptionBox.setValue(existingEmpire.getDescription());
            capitalBox.setValue(existingEmpire.getCapitalName());
            coloniesBox.setValue(String.join("; ", existingEmpire.getColonies()));

            hideField(nameBox);
            hideField(ideologyBox);
            hideField(descriptionBox);
            hideField(capitalBox);
            hideField(coloniesBox);
        }

        if (!editable) {
            nameBox.setEditable(false);
            ideologyBox.setEditable(false);
            descriptionBox.setEditable(false);
            capitalBox.setEditable(false);
            coloniesBox.setEditable(false);
        }

        int buttonY = guiTop + GUI_HEIGHT - 20;

        if (editable) {
            this.addRenderableWidget(Button.builder(
                    Component.literal(existingEmpire == null ? "Create" : "Save"),
                    b -> onSave()
            ).bounds(guiLeft + 20, buttonY, 70, 16).build());
        }

        if (editable && existingEmpire != null) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("Delete"),
                    b -> onDeleteClicked((Button) b)
            ).bounds(guiLeft + 100, buttonY, 70, 16).build());
        }

        if (existingEmpire != null && this.minecraft.player != null
                && !liveEmpire.isMember(this.minecraft.player.getUUID())) {
            String label = liveEmpire.isPublic() ? "Join" : "Request";
            this.addRenderableWidget(Button.builder(
                    Component.literal(label),
                    b -> onJoinClicked()
            ).bounds(guiLeft + GUI_WIDTH - 170, buttonY, 70, 16).build());
        } else if (existingEmpire != null && this.minecraft.player != null
                && liveEmpire.isMember(this.minecraft.player.getUUID())
                && !liveEmpire.isEmperor(this.minecraft.player.getUUID())) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("Leave"),
                    b -> onLeaveClicked()
            ).bounds(guiLeft + GUI_WIDTH - 170, buttonY, 70, 16).build());
        }

        this.addRenderableWidget(Button.builder(
                Component.literal("Close"),
                b -> this.onClose()
        ).bounds(guiLeft + GUI_WIDTH - 90, buttonY, 70, 16).build());
    }

    private EditBox makeEditBox(int x1, int y1, int x2, int y2, int maxLength, String hint) {
        EditBox box = new EditBox(this.font, guiLeft + x1, guiTop + y1,
                x2 - x1, Math.max(10, y2 - y1), Component.literal(hint));
        box.setMaxLength(maxLength);
        box.setHint(Component.literal(hint));
        this.addRenderableWidget(box);
        return box;
    }

    private void hideField(EditBox box) {
        box.setVisible(false);
        box.active = false;
        box.setFocused(false);
    }

    private void showField(EditBox box) {
        box.setVisible(true);
        box.active = true;
        box.setFocused(true);
    }

    private void syncNewRequestsIntoPanel() {
        if (!showingRequests || liveEmpire == null) return;

        List<UUID> liveUUIDs = liveEmpire.getPendingRequestUUIDs();
        List<String> liveNames = liveEmpire.getPendingRequestNames();
        List<Long> liveTimestamps = liveEmpire.getPendingRequestTimestamps();

        boolean changed = false;
        for (int i = 0; i < liveUUIDs.size(); i++) {
            UUID uuid = liveUUIDs.get(i);
            if (!localPendingUUIDs.contains(uuid)) {
                localPendingUUIDs.add(uuid);
                localPendingNames.add(liveNames.get(i));
                localPendingTimestamps.add(liveTimestamps.get(i));
                changed = true;
            }
        }
        if (changed) rebuildRequestRows();
    }

    private byte[] pendingFlagBytes;

    private void openFlagUploadDialog() {
        new Thread(() -> {
            String path = org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_openFileDialog(
                    "Select a flag image", null,
                    null, "Images (*.png, *.jpg, *.jpeg)", false);

            if (path == null) return;

            java.awt.image.BufferedImage buffered;
            try {
                buffered = javax.imageio.ImageIO.read(java.nio.file.Path.of(path).toFile());
                if (buffered == null) {
                    this.minecraft.execute(() -> this.minecraft.player.displayClientMessage(
                            Component.literal("§cCouldn't read that file as an image."), false));
                    return;
                }
            } catch (Exception e) {
                EmpariumRegistry.LOGGER.warn("Flag upload failed (ImageIO)", e);
                this.minecraft.execute(() -> this.minecraft.player.displayClientMessage(
                        Component.literal("§cCouldn't read that file."), false));
                return;
            }

            java.awt.image.BufferedImage argbImage =
                    new java.awt.image.BufferedImage(buffered.getWidth(), buffered.getHeight(),
                            java.awt.image.BufferedImage.TYPE_INT_ARGB);

            java.awt.Graphics2D g0 = argbImage.createGraphics();
            g0.drawImage(buffered, 0, 0, null);
            g0.dispose();

            buffered = argbImage;

            int width = buffered.getWidth();
            int height = buffered.getHeight();

            if (width > MAX_FLAG_DIMENSION || height > MAX_FLAG_DIMENSION) {
                float scale = MAX_FLAG_DIMENSION / (float) Math.max(width, height);
                int newW = Math.max(1, Math.round(width * scale));
                int newH = Math.max(1, Math.round(height * scale));

                java.awt.image.BufferedImage scaled =
                        new java.awt.image.BufferedImage(newW, newH, java.awt.image.BufferedImage.TYPE_INT_ARGB);

                java.awt.Graphics2D g = scaled.createGraphics();
                g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                        java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(buffered, 0, 0, newW, newH, null);
                g.dispose();

                buffered = scaled;
            }

            java.awt.image.BufferedImage finalBuffered = buffered;

            this.minecraft.execute(() -> {
                try {
                    int w = finalBuffered.getWidth();
                    int h = finalBuffered.getHeight();

                    NativeImage nativeImg = new NativeImage(w, h, true);

                    for (int y = 0; y < h; y++) {
                        for (int x = 0; x < w; x++) {
                            int argb = finalBuffered.getRGB(x, y);

                            int abgr =
                                    (argb & 0xFF00FF00) |
                                            ((argb & 0x00FF0000) >>> 16) |
                                            ((argb & 0x000000FF) << 16);

                            nativeImg.setPixelRGBA(x, y, abgr);
                        }
                    }


                    byte[] pngBytes = nativeImg.asByteArray();
                    nativeImg.close();

                    if (pngBytes.length > UploadFlagChunkC2SPacket.MAX_TOTAL_BYTES) {
                        this.minecraft.player.displayClientMessage(
                                Component.literal("§cThat image is too large even after resizing — try a simpler image."),
                                false);
                        return;
                    }

                    if (existingEmpire == null) {
                        pendingFlagBytes = pngBytes;
                        this.minecraft.player.displayClientMessage(
                                Component.literal("§eFlag selected — it'll upload once the empire is created."),
                                false);
                    } else {
                        sendFlagChunks(existingEmpire.getName(), pngBytes);
                    }

                } catch (Exception e) {
                    EmpariumRegistry.LOGGER.warn("Flag upload failed (NativeImage conversion)", e);
                    this.minecraft.player.displayClientMessage(
                            Component.literal("§cCouldn't process that image."), false);
                }
            });

        }, "emparium-flag-upload").start();
    }





    public static void sendFlagChunks(String empireName, byte[] pngBytes) {
        UUID uploadId = UUID.randomUUID();
        int totalChunks = (int) Math.ceil(pngBytes.length / (double) UploadFlagChunkC2SPacket.CHUNK_SIZE);

        for (int i = 0; i < totalChunks; i++) {
            int start = i * UploadFlagChunkC2SPacket.CHUNK_SIZE;
            int end = Math.min(start + UploadFlagChunkC2SPacket.CHUNK_SIZE, pngBytes.length);
            byte[] chunk = java.util.Arrays.copyOfRange(pngBytes, start, end);

            PacketDistributor.sendToServer(
                    new UploadFlagChunkC2SPacket(empireName, uploadId, i, totalChunks, chunk));
        }
    }

    private static com.mojang.blaze3d.platform.NativeImage resizeDownTo(
            com.mojang.blaze3d.platform.NativeImage source, int maxDimension) {
        int width = source.getWidth();
        int height = source.getHeight();

        if (width <= maxDimension && height <= maxDimension) {
            com.mojang.blaze3d.platform.NativeImage copy =
                    new com.mojang.blaze3d.platform.NativeImage(width, height, false);
            source.resizeSubRectTo(0, 0, width, height, copy);
            return copy;
        }

        float scale = maxDimension / (float) Math.max(width, height);
        int newWidth = Math.max(1, Math.round(width * scale));
        int newHeight = Math.max(1, Math.round(height * scale));

        com.mojang.blaze3d.platform.NativeImage resized =
                new com.mojang.blaze3d.platform.NativeImage(newWidth, newHeight, false);
        source.resizeSubRectTo(0, 0, width, height, resized);
        return resized;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (existingEmpire != null && editable) {
            if (tryActivateField(mouseX, mouseY, nameBox, NAME_X1, NAME_Y1, NAME_X2, NAME_Y2)) return true;
            if (tryActivateField(mouseX, mouseY, ideologyBox, IDEOLOGY_X1, IDEOLOGY_Y1, IDEOLOGY_X2, IDEOLOGY_Y2)) return true;
            if (tryActivateField(mouseX, mouseY, descriptionBox, DESC_X1, DESC_Y1, DESC_X2, DESC_BOX_Y2)) return true;
            if (tryActivateField(mouseX, mouseY, capitalBox, CAPITAL_X1, CAPITAL_Y1, CAPITAL_X2, CAPITAL_Y2)) return true;
            if (tryActivateField(mouseX, mouseY, coloniesBox, COLONIES_X1, COLONIES_Y1, COLONIES_X2, COLONIES_Y2)) return true;
            if (editable && tryHandleKickClick(mouseX, mouseY)) return true;
        }
        if (editable && isWithin(mouseX, mouseY,
                guiLeft + PUBLIC_BTN_X1, guiTop + PUBLIC_BTN_Y1, guiLeft + PUBLIC_BTN_X2, guiTop + PUBLIC_BTN_Y2)) {
            isPublicSelection = !isPublicSelection;
            return true;
        }

        if (editable && tryHandleRatioClick(mouseX, mouseY)) return true;

        if (editable && existingEmpire != null && isWithin(mouseX, mouseY,
                guiLeft + REQUESTS_BTN_X1, guiTop + REQUESTS_BTN_Y1, guiLeft + REQUESTS_BTN_X2, guiTop + REQUESTS_BTN_Y2)) {
            toggleRequestsPanel();
            return true;
        }

        if (editable && isWithin(mouseX, mouseY,
                guiLeft + FLAG_BTN_X1, guiTop + FLAG_BTN_Y1, guiLeft + FLAG_BTN_X2, guiTop + FLAG_BTN_Y2)) {
            openFlagUploadDialog();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean tryActivateField(double mouseX, double mouseY, EditBox box, int x1, int y1, int x2, int y2) {
        if (box.isVisible()) return false;
        if (!isWithin(mouseX, mouseY, guiLeft + x1, guiTop + y1, guiLeft + x2, guiTop + y2)) return false;
        showField(box);
        return true;
    }

    private void revertIfUnfocused(EditBox box) {
        if (box.isVisible() && !box.isFocused()) {
            box.setVisible(false);
            box.active = false;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (existingEmpire != null) {
            revertIfUnfocused(nameBox);
            revertIfUnfocused(ideologyBox);
            revertIfUnfocused(descriptionBox);
            revertIfUnfocused(capitalBox);
            revertIfUnfocused(coloniesBox);
        }

        if (existingEmpire != null) {
            EmpireData refreshed = ClientEmpireCache.getEmpireByName(existingEmpire.getName());
            if (refreshed != null) liveEmpire = refreshed;
            syncNewRequestsIntoPanel();
        }

        drawBackgroundTexture(graphics);

        graphics.drawString(this.font,
                Component.literal(existingEmpire == null ? "Form Empire"
                        : editable ? "Edit Empire" : "Empire Details"),
                guiLeft + 20, guiTop + 8, 0xFFB87333);

        drawFlagButton(graphics, mouseX, mouseY);
        drawEmperorModel(graphics, mouseX, mouseY);
        drawMemberSlots(graphics, mouseX, mouseY);
        drawFoundedAndPlaytime(graphics);
        drawPublicToggleButton(graphics, mouseX, mouseY);
        drawRatioButtons(graphics, mouseX, mouseY);
        drawRequestsButton(graphics, mouseX, mouseY);
        drawRequestsPanel(graphics);

        renderFieldText(graphics, nameBox, NAME_X1, NAME_Y1, NAME_X2 - NAME_X1, NAME_Y2 - NAME_Y1, "Name");
        renderFieldText(graphics, ideologyBox, IDEOLOGY_X1, IDEOLOGY_Y1, IDEOLOGY_X2 - IDEOLOGY_X1, IDEOLOGY_Y2 - IDEOLOGY_Y1, "Ideology");
        renderFieldText(graphics, descriptionBox, DESC_TEXT_X1, DESC_TEXT_Y1, DESC_X2 - DESC_X1, DESC_Y2 - DESC_Y1, "Description");
        renderFieldText(graphics, capitalBox, CAPITAL_TEXT_X1, CAPITAL_TEXT_Y1, CAPITAL_X2 - CAPITAL_X1, CAPITAL_Y2 - CAPITAL_Y1, "Capital name");
        renderFieldText(graphics, coloniesBox, COLONIES_TEXT_X1, COLONIES_TEXT_Y1, COLONIES_X2 - COLONIES_X1, COLONIES_Y2 - COLONIES_Y1, "Colonies");

        if (!editable) {
            graphics.drawString(this.font,
                    Component.literal("View only — you don't have permission to edit this empire."),
                    guiLeft + 20, guiTop + GUI_HEIGHT - 42, 0xFFFF6666);
        }

        if (confirmingDelete) {
            graphics.drawString(this.font,
                    Component.literal("Click Delete again to confirm."),
                    guiLeft + 100, guiTop + GUI_HEIGHT - 34, 0xFFFF6666);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderFieldText(GuiGraphics graphics, EditBox box, int x1, int y1, int fieldWidth,
                                 int maxHeight, String placeholder) {
        if (box.isVisible()) return;

        String value = box.getValue();
        boolean empty = value.isEmpty();
        String display = empty ? "(" + placeholder + ")" : value;
        int color = empty ? 0xFF666666 : 0xFFFFFFFF;

        int wrapWidth = (int) (fieldWidth / INLINE_TEXT_SCALE);
        List<net.minecraft.util.FormattedCharSequence> lines =
                this.font.split(Component.literal(display), wrapWidth);

        int maxLines = Math.max(1, (int) (maxHeight / (INLINE_LINE_HEIGHT * INLINE_TEXT_SCALE)));

        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(guiLeft + x1, guiTop + y1, 0);
        pose.scale(INLINE_TEXT_SCALE, INLINE_TEXT_SCALE, 1f);

        for (int i = 0; i < lines.size() && i < maxLines; i++) {
            graphics.drawString(this.font, lines.get(i), 1, 1 + i * INLINE_LINE_HEIGHT, color);
        }

        pose.popPose();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    private void drawBackgroundTexture(GuiGraphics graphics) {
        graphics.blit(BACKGROUND_TEXTURE, guiLeft, guiTop, 0, 0,
                GUI_WIDTH, GUI_HEIGHT, GUI_TEXTURE_WIDTH, GUI_TEXTURE_HEIGHT);
    }

    private void drawFlagButton(GuiGraphics graphics, int mouseX, int mouseY) {
        int x1 = guiLeft + FLAG_BTN_X1, y1 = guiTop + FLAG_BTN_Y1;
        int x2 = guiLeft + FLAG_BTN_X2, y2 = guiTop + FLAG_BTN_Y2;

        ResourceLocation flagTexture = liveEmpire != null
                ? ClientFlagCache.getFlagTexture(liveEmpire.getEmpireId())
                : null;

        if (flagTexture == null && pendingFlagBytes != null) {
            flagTexture = ClientFlagCache.getOrCreatePreviewTexture(pendingFlagBytes);
        }

        if (flagTexture != null) {
            var rect = AspectFit.fit(x1, y1, x2, y2, flagRatioSelection.widthToHeight);
            int w = rect.x2() - rect.x1();
            int h = rect.y2() - rect.y1();
            graphics.blit(flagTexture, rect.x1(), rect.y1(), 0, 0, w, h, w, h);
        }

        boolean hovered = editable && isWithin(mouseX, mouseY, x1, y1, x2, y2);
        graphics.fill(x1, y1, x2, y2, hovered ? 0x33FFFFFF : 0x00000000);
    }

    private void drawEmperorModel(GuiGraphics graphics, int mouseX, int mouseY) {
        if (liveEmpire == null) return;

        int x1 = guiLeft + MODEL_X1, y1 = guiTop + MODEL_Y1;
        int x2 = guiLeft + MODEL_X2, y2 = guiTop + MODEL_Y2;

        LivingEntity renderTarget = null;
        if (this.minecraft.level != null) {
            Player p = this.minecraft.level.getPlayerByUUID(liveEmpire.getEmperorUUID());
            if (p instanceof LivingEntity living) {
                renderTarget = living;
            }
        }

        if (renderTarget != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    graphics, x1, y1, x2, y2, 30, 0.0625f, mouseX, mouseY, renderTarget);
        } else {
            graphics.fill(x1, y1, x2, y2, 0xFF1E1E1E);
            graphics.drawString(this.font, Component.literal("?"),
                    x1 + (x2 - x1) / 2 - 2, y1 + (y2 - y1) / 2 - 4, 0xFF555555);
        }
    }

    private void drawMemberSlots(GuiGraphics graphics) {
        List<String> names = new ArrayList<>();
        if (liveEmpire != null) {
            names.add(liveEmpire.getEmperorName());
            names.addAll(liveEmpire.getMemberNames());
        }

        int slotHeight = (MEMBERS_Y2 - MEMBERS_Y1) / MEMBER_SLOT_COUNT;

        for (int i = 0; i < MEMBER_SLOT_COUNT; i++) {
            int slotY = guiTop + MEMBERS_Y1 + i * slotHeight;
            String label = i < names.size() ? names.get(i) : "";
            int color = i == 0 ? 0xFFB87333 : 0xFFAAAAAA;

            var pose = graphics.pose();
            pose.pushPose();
            pose.translate(guiLeft + MEMBERS_X1, slotY, 0);
            pose.scale(0.7f, 0.7f, 1f);
            graphics.drawString(this.font, Component.literal(label), 1, 2, color);
            pose.popPose();
        }
    }

    private void drawFoundedAndPlaytime(GuiGraphics graphics) {
        if (liveEmpire == null) return;

        String founded = new SimpleDateFormat("MM/dd/yy")
                .format(new Date(liveEmpire.getFoundedEpochMillis()));
        String playtime = PlaytimeUtil.formatTicksAsDuration(liveEmpire.getTotalPlaytimeTicks());

        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(guiLeft + INFO_X1, guiTop + INFO_Y1, 0);
        pose.scale(0.6f, 0.6f, 1f);
        graphics.drawString(this.font, Component.literal(founded), 1, 1, 0xFF888888);
        graphics.drawString(this.font, Component.literal(playtime), 1, 12, 0xFF888888);
        pose.popPose();
    }

    private void drawRatioButtons(GuiGraphics graphics, int mouseX, int mouseY) {
        String[] labels = {"16:9", "1:1", "2:1"};
        FlagRatio[] options = FlagRatio.values();

        for (int i = 0; i < options.length; i++) {
            int[] b = RATIO_BUTTON_BOUNDS.get(options[i]);
            int x1 = guiLeft + b[0], y1 = guiTop + b[1], x2 = guiLeft + b[2], y2 = guiTop + b[3];

            boolean selected = flagRatioSelection == options[i];
            boolean hovered = editable && isWithin(mouseX, mouseY, x1, y1, x2, y2);
            graphics.fill(x1, y1, x2, y2, selected ? 0x55B87333 : hovered ? 0x33FFFFFF : 0x33222222);
            graphics.drawString(this.font, Component.literal(labels[i]), x1 + 2, y1 + 2, 0xFFCCCCCC);
        }
    }

    private boolean tryHandleRatioClick(double mouseX, double mouseY) {
        for (FlagRatio ratio : FlagRatio.values()) {
            int[] b = RATIO_BUTTON_BOUNDS.get(ratio);
            int x1 = guiLeft + b[0], y1 = guiTop + b[1], x2 = guiLeft + b[2], y2 = guiTop + b[3];
            if (isWithin(mouseX, mouseY, x1, y1, x2, y2)) {
                flagRatioSelection = ratio;
                return true;
            }
        }
        return false;
    }

    private void onSave() {
        String name = nameBox.getValue().trim();
        String ideology = ideologyBox.getValue().trim();
        String description = descriptionBox.getValue().trim();
        String capital = capitalBox.getValue().trim();
        String colonies = coloniesBox.getValue().trim();

        if (name.isEmpty()) {
            nameBox.setTextColor(0xFFFF5555);
            showField(nameBox);
            return;
        }

        if (existingEmpire == null) {
            if (pendingFlagBytes != null) {
                ClientPendingFlagUploads.queue(name, pendingFlagBytes);
            }
            PacketDistributor.sendToServer(
                    new CreateEmpireC2SPacket(name, ideology, capital, "", description,
                            flagRatioSelection.name(), isPublicSelection));
        } else {
            PacketDistributor.sendToServer(
                    new UpdateEmpireC2SPacket(existingEmpire.getName(), name, ideology, capital,
                            existingEmpire.getFlagUrl(), description, colonies,
                            flagRatioSelection.name(), isPublicSelection));
        }

        this.onClose();
    }

    private void onDeleteClicked(Button source) {
        if (existingEmpire == null) return;

        if (!confirmingDelete) {
            confirmingDelete = true;
            source.setMessage(Component.literal("Confirm?"));
            return;
        }

        PacketDistributor.sendToServer(new DeleteEmpireC2SPacket(existingEmpire.getName()));
        this.onClose();
    }

    private void onJoinClicked() {
        if (existingEmpire == null) return;
        PacketDistributor.sendToServer(new JoinEmpireC2SPacket(existingEmpire.getName()));
        this.onClose();
    }

    private void onLeaveClicked() {
        PacketDistributor.sendToServer(new LeaveEmpireC2SPacket());
        this.onClose();
    }

    private void drawPublicToggleButton(GuiGraphics graphics, int mouseX, int mouseY) {
        int x1 = guiLeft + PUBLIC_BTN_X1, y1 = guiTop + PUBLIC_BTN_Y1;
        int x2 = guiLeft + PUBLIC_BTN_X2, y2 = guiTop + PUBLIC_BTN_Y2;
        boolean hovered = editable && isWithin(mouseX, mouseY, x1, y1, x2, y2);
        graphics.fill(x1, y1, x2, y2, hovered ? 0x66222222 : 0x33222222);

        String label = isPublicSelection ? "Public" : "Private";
        int color = isPublicSelection ? 0xFF55FF55 : 0xFFFF5555;
        graphics.drawString(this.font, Component.literal(label), x1 + 4, y1 + 3, color);
    }

    private void drawRequestsButton(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!editable || liveEmpire == null) return;
        int x1 = guiLeft + REQUESTS_BTN_X1, y1 = guiTop + REQUESTS_BTN_Y1;
        int x2 = guiLeft + REQUESTS_BTN_X2, y2 = guiTop + REQUESTS_BTN_Y2;
        boolean hovered = isWithin(mouseX, mouseY, x1, y1, x2, y2);
        graphics.fill(x1, y1, x2, y2, hovered ? 0x66222222 : 0x33222222);

        int pendingCount = liveEmpire.getPendingRequestUUIDs().size();
        if (pendingCount > 0) {
            graphics.drawString(this.font, Component.literal(String.valueOf(pendingCount)), x2 - 10, y1 + 3, 0xFFFF5555);
        }
    }

    private void toggleRequestsPanel() {
        if (showingRequests) closeRequestsPanel(); else openRequestsPanel();
    }

    private void openRequestsPanel() {
        if (liveEmpire == null) return;
        localPendingUUIDs.clear();
        localPendingUUIDs.addAll(liveEmpire.getPendingRequestUUIDs());
        localPendingNames.clear();
        localPendingNames.addAll(liveEmpire.getPendingRequestNames());
        localPendingTimestamps.clear();
        localPendingTimestamps.addAll(liveEmpire.getPendingRequestTimestamps());
        showingRequests = true;
        rebuildRequestRows();
    }

    private void closeRequestsPanel() {
        showingRequests = false;
        for (var widget : requestRowWidgets) this.removeWidget(widget);
        requestRowWidgets.clear();
    }

    private void rebuildRequestRows() {
        for (var widget : requestRowWidgets) this.removeWidget(widget);
        requestRowWidgets.clear();

        int panelX = guiLeft + GUI_WIDTH;
        int rowY = guiTop + 24;

        for (int i = 0; i < localPendingUUIDs.size(); i++) {
            UUID targetUUID = localPendingUUIDs.get(i);
            int y = rowY + i * REQUESTS_ROW_HEIGHT;

            var acceptBtn = Button.builder(Component.literal("Accept"), b -> respondToRequest(targetUUID, true))
                    .bounds(panelX + 6, y + 10, 50, 14).build();
            var denyBtn = Button.builder(Component.literal("Deny"), b -> respondToRequest(targetUUID, false))
                    .bounds(panelX + 60, y + 10, 50, 14).build();

            this.addRenderableWidget(acceptBtn);
            this.addRenderableWidget(denyBtn);
            requestRowWidgets.add(acceptBtn);
            requestRowWidgets.add(denyBtn);
        }
    }

    private void respondToRequest(UUID targetUUID, boolean accept) {
        if (existingEmpire == null) return;
        if (accept) {
            PacketDistributor.sendToServer(new AcceptJoinRequestC2SPacket(existingEmpire.getName(), targetUUID));
        } else {
            PacketDistributor.sendToServer(new DenyJoinRequestC2SPacket(existingEmpire.getName(), targetUUID));
        }

        int index = localPendingUUIDs.indexOf(targetUUID);
        if (index != -1) {
            localPendingUUIDs.remove(index);
            localPendingNames.remove(index);
            localPendingTimestamps.remove(index);
        }
        rebuildRequestRows();
    }

    private void drawRequestsPanel(GuiGraphics graphics) {
        if (!showingRequests) return;

        int panelX = guiLeft + GUI_WIDTH;
        int panelY = guiTop;

        graphics.fill(panelX, panelY, panelX + REQUESTS_PANEL_WIDTH, panelY + GUI_HEIGHT, 0xEE1A1A1A);
        graphics.drawString(this.font, Component.literal("Join Requests"), panelX + 8, panelY + 8, 0xFFB87333);

        if (localPendingNames.isEmpty()) {
            graphics.drawString(this.font, Component.literal("No pending requests"), panelX + 8, panelY + 30, 0xFF888888);
            return;
        }

        double scale = this.minecraft.getWindow().getGuiScale();
        RenderSystem.enableScissor(
                (int) (panelX * scale), (int) ((this.height - (panelY + GUI_HEIGHT)) * scale),
                (int) (REQUESTS_PANEL_WIDTH * scale), (int) (GUI_HEIGHT * scale));

        for (int i = 0; i < localPendingNames.size(); i++) {
            int y = panelY + 24 + i * REQUESTS_ROW_HEIGHT;
            String label = localPendingNames.get(i) + "  §7(" + formatTimeAgo(localPendingTimestamps.get(i)) + ")";
            graphics.drawString(this.font, Component.literal(label), panelX + 8, y, 0xFFFFFFFF);
        }

        RenderSystem.disableScissor();
    }

    private UUID confirmingKickUUID = null;

    private void drawMemberSlots(GuiGraphics graphics, int mouseX, int mouseY) {
        List<String> names = new ArrayList<>();
        List<UUID> uuids = new ArrayList<>();
        if (liveEmpire != null) {
            names.add(liveEmpire.getEmperorName());
            uuids.add(liveEmpire.getEmperorUUID());
            names.addAll(liveEmpire.getMemberNames());
            uuids.addAll(liveEmpire.getMemberUUIDs());
        }

        int slotHeight = (MEMBERS_Y2 - MEMBERS_Y1) / MEMBER_SLOT_COUNT;
        double scale = this.minecraft.getWindow().getGuiScale();

        for (int i = 0; i < MEMBER_SLOT_COUNT; i++) {
            int slotY = guiTop + MEMBERS_Y1 + i * slotHeight;
            String label = i < names.size() ? names.get(i) : "";
            boolean isEmperorSlot = i == 0;
            int color = isEmperorSlot ? 0xFFB87333 : 0xFFAAAAAA;

            boolean canKickThis = editable && !isEmperorSlot && i < uuids.size();
            boolean hoveredRow = canKickThis && isWithin(mouseX, mouseY,
                    guiLeft + MEMBERS_X1, slotY, guiLeft + MEMBERS_X2, slotY + slotHeight);

            String display = label;
            if (canKickThis && hoveredRow) {
                boolean armed = confirmingKickUUID != null && confirmingKickUUID.equals(uuids.get(i));
                display = label + (armed ? " [confirm?]" : " [x]");
            }

            RenderSystem.enableScissor(
                    (int) ((guiLeft + MEMBERS_X1) * scale), (int) ((this.height - (slotY + slotHeight)) * scale),
                    (int) ((MEMBERS_X2 - MEMBERS_X1) * scale), (int) (slotHeight * scale));

            var pose = graphics.pose();
            pose.pushPose();
            pose.translate(guiLeft + MEMBERS_X1, slotY, 0);
            pose.scale(0.7f, 0.7f, 1f);
            graphics.drawString(this.font, Component.literal(display), 1, 2, color);
            pose.popPose();

            RenderSystem.disableScissor();
        }
    }

    private boolean tryHandleKickClick(double mouseX, double mouseY) {
        if (liveEmpire == null) return false;
        List<UUID> memberUUIDs = liveEmpire.getMemberUUIDs();
        int slotHeight = (MEMBERS_Y2 - MEMBERS_Y1) / MEMBER_SLOT_COUNT;

        for (int i = 0; i < memberUUIDs.size(); i++) {
            int slotIndex = i + 1;
            if (slotIndex >= MEMBER_SLOT_COUNT) break;
            int slotY = guiTop + MEMBERS_Y1 + slotIndex * slotHeight;

            if (isWithin(mouseX, mouseY, guiLeft + MEMBERS_X1, slotY, guiLeft + MEMBERS_X2, slotY + slotHeight)) {
                UUID target = memberUUIDs.get(i);
                if (confirmingKickUUID != null && confirmingKickUUID.equals(target)) {
                    PacketDistributor.sendToServer(new KickMemberC2SPacket(liveEmpire.getName(), target));
                    confirmingKickUUID = null;
                } else {
                    confirmingKickUUID = target;
                }
                return true;
            }
        }
        return false;
    }

    private static String formatTimeAgo(long epochMillis) {
        long elapsedSeconds = (System.currentTimeMillis() - epochMillis) / 1000;
        if (elapsedSeconds < 60) return elapsedSeconds + "s ago";
        long minutes = elapsedSeconds / 60;
        if (minutes < 60) return minutes + "m ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h ago";
        return (hours / 24) + "d ago";
    }

    private static boolean isWithin(double mouseX, double mouseY, int x1, int y1, int x2, int y2) {
        return mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}