package net.tommybutz.emparium_registry.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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
import net.tommybutz.emparium_registry.network.ClientEmpireCache;
import net.tommybutz.emparium_registry.network.ClientFlagCache;
import net.tommybutz.emparium_registry.network.DeleteEmpireC2SPacket;
import net.tommybutz.emparium_registry.util.AspectFit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class RegistryScreen extends Screen {

    private static final ResourceLocation BACKGROUND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EmpariumRegistry.MODID, "textures/gui/registry_background.png");
    private static final ResourceLocation CARD_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EmpariumRegistry.MODID, "textures/gui/empire_card.png");

    private static final int GUI_WIDTH = 300;
    private static final int GUI_HEIGHT = 330;

    private static final int GUI_TEXTURE_WIDTH = 300;
    private static final int GUI_TEXTURE_HEIGHT = 330;

    private static final int SIDEBAR_WIDTH = 100;

    private static final int CARD_WIDTH = 130;
    private static final int CARD_HEIGHT = 165;

    private static final int CARD_TEXTURE_WIDTH = 130;
    private static final int CARD_TEXTURE_HEIGHT = 165;

    private static final int CARD_PADDING = 8;
    private static final int CARD_TOP_PADDING = 10;

    private static final int CARD_AREA_X1 = 119;
    private static final int CARD_AREA_Y1 = 35;
    private static final int CARD_AREA_X2 = 268;
    private static final int CARD_AREA_Y2 = 259;
    private static final int CARD_AREA_WIDTH = CARD_AREA_X2 - CARD_AREA_X1;
    private static final int CARD_AREA_HEIGHT = CARD_AREA_Y2 - CARD_AREA_Y1;

    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_GAP = 2;

    private static final int FLAG_X1 = 15, FLAG_Y1 = 15, FLAG_X2 = 113, FLAG_Y2 = 66;
    private static final int SKIN_X1 = 14, SKIN_Y1 = 82, SKIN_X2 = 41, SKIN_Y2 = 120;
    private static final int NAME_X1 = 48, NAME_Y1 = 82, NAME_X2 = 114, NAME_Y2 = 99;
    private static final int IDEOLOGY_X1 = 48, IDEOLOGY_Y1 = 103, IDEOLOGY_X2 = 114, IDEOLOGY_Y2 = 120;
    private static final int EMPEROR_X1 = 12, EMPEROR_Y1 = 132, EMPEROR_X2 = 38, EMPEROR_Y2 = 153;
    private static final int MEMBERS_X1 = 52, MEMBERS_Y1 = 132, MEMBERS_X2 = 115, MEMBERS_Y2 = 153;

    private static final int SIDEBAR_INSET = 14;
    private static final int SIDEBAR_CONTENT_WIDTH = SIDEBAR_WIDTH - (SIDEBAR_INSET * 2);

    private static final int SEARCH_X = 14;
    private static final int SEARCH_Y = 37;
    private static final int SEARCH_HEIGHT = 12;

    private static final int SORT_X = 14;
    private static final int SORT_FIRST_Y = 100;
    private static final int SORT_BUTTON_SPACING = 13;
    private static final int SORT_BUTTON_HEIGHT = 10;

    private static final int RATIO_LABEL_X = 14;
    private static final int RATIO_LABEL_Y = 160;

    private static final int FORM_BTN_X1 = 16;
    private static final int FORM_BTN_Y1 = 276;
    private static final int FORM_BTN_X2 = 283;
    private static final int FORM_BTN_Y2 = 323;

    private static final int ADMIN_BTN_X1 = 235;
    private static final int ADMIN_BTN_Y1 = 5;
    private static final int ADMIN_BTN_WIDTH = 52;
    private static final int ADMIN_BTN_HEIGHT = 22;
    private static final int ADMIN_BTN_X2 = ADMIN_BTN_X1 + ADMIN_BTN_WIDTH;
    private static final int ADMIN_BTN_Y2 = ADMIN_BTN_Y1 + ADMIN_BTN_HEIGHT;

    private static final Map<FlagRatio, int[]> RATIO_BUTTON_BOUNDS = Map.of(
            FlagRatio.SIXTEEN_NINE, new int[]{14, 171, 41, 183},
            FlagRatio.ONE_ONE, new int[]{49, 171, 76, 183},
            FlagRatio.TWO_ONE, new int[]{82, 171, 109, 183}
    );

    private int guiLeft, guiTop;

    private float scrollOffset = 0f;
    private float maxScroll = 0f;
    private boolean isDraggingScrollbar = false;

    private EditBox searchBox;
    private String searchQuery = "";
    private SortMode sortMode = SortMode.MEMBER_COUNT;
    private FlagRatio ratioFilter = null;
    private boolean adminMode = false;

    private List<EmpireData> filteredEmpires = new ArrayList<>();

    public enum SortMode {
        MEMBER_COUNT, CLAIMS_COUNT, DAYS_REGISTERED, ALPHABETICAL
    }

    private record CardLayout(int areaX, int areaY, int areaWidth, int areaHeight,
                              int cardsPerRow, int startX, int startY) {}

    public RegistryScreen() {
        super(Component.translatable("screen.emparium_registry.registry"));
    }

    @Override
    protected void init() {
        guiLeft = (this.width - GUI_WIDTH) / 2;
        guiTop = (this.height - GUI_HEIGHT) / 2;

        searchBox = new EditBox(
                this.font,
                guiLeft + SEARCH_X,
                guiTop + SEARCH_Y,
                SIDEBAR_CONTENT_WIDTH,
                SEARCH_HEIGHT,
                Component.literal("Search...")
        );
        searchBox.setMaxLength(32);
        searchBox.setHint(Component.literal("Search..."));
        searchBox.setResponder(query -> {
            searchQuery = query.toLowerCase();
            refreshEmpires();
        });
        this.addRenderableWidget(searchBox);

        refreshEmpires();
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        guiLeft = (this.width - GUI_WIDTH) / 2;
        guiTop = (this.height - GUI_HEIGHT) / 2;
        searchBox.setX(guiLeft + SEARCH_X);
        searchBox.setY(guiTop + SEARCH_Y);
        recalculateMaxScroll();
    }

    public void refreshEmpires() {
        List<EmpireData> all = new ArrayList<>(ClientEmpireCache.getEmpires());

        if (!searchQuery.isEmpty()) {
            all.removeIf(e -> !e.getName().toLowerCase().contains(searchQuery)
                    && !e.getEmperorName().toLowerCase().contains(searchQuery));
        }

        if (ratioFilter != null) {
            all.removeIf(e -> e.getFlagRatio() != ratioFilter);
        }

        switch (sortMode) {
            case MEMBER_COUNT ->
                    all.sort(Comparator.comparingInt(EmpireData::getMemberCount).reversed());
            case CLAIMS_COUNT ->
                    all.sort(Comparator.comparingInt(EmpireData::getClaimCount).reversed());
            case DAYS_REGISTERED ->
                    all.sort(Comparator.comparingLong(EmpireData::getRegisteredAtTick));
            case ALPHABETICAL ->
                    all.sort(Comparator.comparing(EmpireData::getName));
        }

        filteredEmpires = all;
        recalculateMaxScroll();
    }

    private CardLayout computeCardLayout() {
        int areaX = guiLeft + CARD_AREA_X1;
        int areaY = guiTop + CARD_AREA_Y1;
        int areaWidth = CARD_AREA_WIDTH;
        int areaHeight = CARD_AREA_HEIGHT;
        int cardsPerRow = Math.max(1, areaWidth / (CARD_WIDTH + CARD_PADDING));

        int rowWidth = cardsPerRow * CARD_WIDTH + (cardsPerRow - 1) * CARD_PADDING;

        int startX = areaX + (areaWidth - rowWidth) / 2;
        int startY = areaY + CARD_TOP_PADDING - (int) scrollOffset;
        return new CardLayout(areaX, areaY, areaWidth, areaHeight, cardsPerRow, startX, startY);
    }

    private void recalculateMaxScroll() {
        CardLayout layout = computeCardLayout();
        int rows = (int) Math.ceil((double) filteredEmpires.size() / layout.cardsPerRow());
        int totalHeight = rows * (CARD_HEIGHT + CARD_PADDING) + CARD_TOP_PADDING;
        maxScroll = Math.max(0, totalHeight - layout.areaHeight());
        scrollOffset = Math.min(scrollOffset, maxScroll);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        drawBackgroundTexture(graphics);
        drawTopBar(graphics);
        drawSidebar(graphics, mouseX, mouseY);
        drawCardArea(graphics, mouseX, mouseY, partialTick);
        drawScrollbar(graphics);
        drawFormEmpireButton(graphics, mouseX, mouseY);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    private void drawBackgroundTexture(GuiGraphics graphics) {
        graphics.blit(BACKGROUND_TEXTURE, guiLeft, guiTop, 0, 0,
                GUI_WIDTH, GUI_HEIGHT, GUI_TEXTURE_WIDTH, GUI_TEXTURE_HEIGHT);
    }

    private void drawTopBar(GuiGraphics graphics) {
        graphics.drawString(this.font, Component.literal("Emparium Registry"),
                guiLeft + SIDEBAR_WIDTH + 8, guiTop + 6, 0xFFB87333);
    }

    private void drawSidebar(GuiGraphics graphics, int mouseX, int mouseY) {
        drawSortButton(graphics, "Members", SortMode.MEMBER_COUNT,
                guiLeft + SORT_X, guiTop + SORT_FIRST_Y, mouseX, mouseY);
        drawSortButton(graphics, "Claims", SortMode.CLAIMS_COUNT,
                guiLeft + SORT_X, guiTop + SORT_FIRST_Y + SORT_BUTTON_SPACING, mouseX, mouseY);
        drawSortButton(graphics, "Days", SortMode.DAYS_REGISTERED,
                guiLeft + SORT_X, guiTop + SORT_FIRST_Y + SORT_BUTTON_SPACING * 2, mouseX, mouseY);
        drawSortButton(graphics, "A-Z", SortMode.ALPHABETICAL,
                guiLeft + SORT_X, guiTop + SORT_FIRST_Y + SORT_BUTTON_SPACING * 3, mouseX, mouseY);

        graphics.drawString(this.font, Component.literal("Flag ratio"),
                guiLeft + RATIO_LABEL_X, guiTop + RATIO_LABEL_Y, 0xFF888888);

        for (FlagRatio ratio : FlagRatio.values()) {
            drawRatioButton(graphics, ratio, mouseX, mouseY);
        }

        int adminX1 = guiLeft + ADMIN_BTN_X1;
        int adminY1 = guiTop + ADMIN_BTN_Y1;
        int adminX2 = guiLeft + ADMIN_BTN_X2;
        int adminY2 = guiTop + ADMIN_BTN_Y2;
        boolean adminHovered = isWithin(mouseX, mouseY, adminX1, adminY1, adminX2, adminY2);
        graphics.fill(adminX1, adminY1, adminX2, adminY2, adminHovered ? 0x55222222 : 0x33222222);
        graphics.drawString(this.font, Component.literal("Admin"), adminX1 + 6, adminY1 + 7,
                adminMode ? 0xFFFF5555 : 0xFFAAAAAA);
    }

    private void drawSortButton(GuiGraphics graphics, String label, SortMode mode,
                                int x, int y, int mouseX, int mouseY) {
        boolean selected = sortMode == mode;
        boolean hovered = isWithin(mouseX, mouseY, x, y, x + SIDEBAR_CONTENT_WIDTH, y + SORT_BUTTON_HEIGHT);
        int color = selected ? 0xFFB87333 : hovered ? 0xFFEEEEEE : 0xFFAAAAAA;
        graphics.drawString(this.font, Component.literal(label), x, y, color);
    }

    private void drawRatioButton(GuiGraphics graphics, FlagRatio ratio, int mouseX, int mouseY) {
        int[] bounds = RATIO_BUTTON_BOUNDS.get(ratio);
        int x1 = guiLeft + bounds[0];
        int y1 = guiTop + bounds[1];
        int x2 = guiLeft + bounds[2];
        int y2 = guiTop + bounds[3];

        boolean selected = ratioFilter == ratio;
        boolean hovered = isWithin(mouseX, mouseY, x1, y1, x2, y2);

        if (selected) {
            graphics.fill(x1, y1, x2, y2, 0x55B87333);
        } else if (hovered) {
            graphics.fill(x1, y1, x2, y2, 0x33FFFFFF);
        }
    }

    private void drawFormEmpireButton(GuiGraphics graphics, int mouseX, int mouseY) {
        int x1 = guiLeft + FORM_BTN_X1;
        int y1 = guiTop + FORM_BTN_Y1;
        int x2 = guiLeft + FORM_BTN_X2;
        int y2 = guiTop + FORM_BTN_Y2;

        boolean hovered = isWithin(mouseX, mouseY, x1, y1, x2, y2);
        graphics.fill(x1, y1, x2, y2, hovered ? 0x66222222 : 0x33222222);
    }

    private void drawCardArea(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        CardLayout layout = computeCardLayout();

        double scale = this.minecraft.getWindow().getGuiScale();
        RenderSystem.enableScissor(
                (int) (layout.areaX() * scale),
                (int) ((this.height - (layout.areaY() + layout.areaHeight())) * scale),
                (int) (layout.areaWidth() * scale),
                (int) (layout.areaHeight() * scale)
        );

        for (int i = 0; i < filteredEmpires.size(); i++) {
            EmpireData empire = filteredEmpires.get(i);
            int col = i % layout.cardsPerRow();
            int row = i / layout.cardsPerRow();
            int cardX = layout.startX() + col * (CARD_WIDTH + CARD_PADDING);
            int cardY = layout.startY() + row * (CARD_HEIGHT + CARD_PADDING);

            if (cardY + CARD_HEIGHT >= layout.areaY() && cardY <= layout.areaY() + layout.areaHeight()) {
                drawEmpireCard(graphics, empire, cardX, cardY, mouseX, mouseY, partialTick);
            }
        }

        if (filteredEmpires.isEmpty()) {
            graphics.drawString(this.font, Component.literal("No empires found"),
                    layout.areaX() + layout.areaWidth() / 2 - 40,
                    layout.areaY() + layout.areaHeight() / 2,
                    0xFF666666);
        }

        RenderSystem.disableScissor();
    }

    private void drawEmpireCard(GuiGraphics graphics, EmpireData empire,
                                int x, int y, int mouseX, int mouseY, float partialTick) {
        boolean hovered = isWithin(mouseX, mouseY, x, y, x + CARD_WIDTH, y + CARD_HEIGHT);

        graphics.blit(CARD_TEXTURE, x, y, 0, 0,
                CARD_WIDTH, CARD_HEIGHT, CARD_TEXTURE_WIDTH, CARD_TEXTURE_HEIGHT);

        if (hovered) {
            graphics.fill(x, y, x + CARD_WIDTH, y + CARD_HEIGHT, 0x22000000);
        }

        ResourceLocation flagTexture = ClientFlagCache.getFlagTexture(empire.getEmpireId());
        if (flagTexture != null) {
            var rect = AspectFit.fit(x + FLAG_X1, y + FLAG_Y1, x + FLAG_X2, y + FLAG_Y2,
                    empire.getFlagRatio().widthToHeight);
            int w = rect.x2() - rect.x1();
            int h = rect.y2() - rect.y1();
            graphics.blit(flagTexture, rect.x1(), rect.y1(), 0, 0, w, h, w, h);
        } else {
            graphics.fill(x + FLAG_X1, y + FLAG_Y1, x + FLAG_X2, y + FLAG_Y2, 0xFF1A1A1A);
            graphics.drawString(this.font, Component.literal("Flag"),
                    x + (FLAG_X1 + FLAG_X2) / 2 - 8, y + (FLAG_Y1 + FLAG_Y2) / 2 - 4, 0xFF555555);
        }

        drawEmperorModel(graphics, empire, x, y, mouseX, mouseY, partialTick);

        String displayName = empire.getName().length() > 12
                ? empire.getName().substring(0, 11) + "…"
                : empire.getName();
        graphics.drawString(this.font, Component.literal(displayName),
                x + NAME_X1 + 2, y + NAME_Y1 + 2, 0xFFFFFFFF);

        String displayIdeology = empire.getIdeology().length() > 12
                ? empire.getIdeology().substring(0, 11) + "…"
                : empire.getIdeology();
        graphics.drawString(this.font, Component.literal(displayIdeology),
                x + IDEOLOGY_X1 + 2, y + IDEOLOGY_Y1 + 2, 0xFFB87333);

        drawScaledCentered(graphics, empire.getEmperorName(),
                x + EMPEROR_X1, y + EMPEROR_Y1, EMPEROR_X2 - EMPEROR_X1, EMPEROR_Y2 - EMPEROR_Y1,
                0.4f, 0xFFAAAAAA);

        drawScaledCentered(graphics, "Members: " + empire.getMemberCount() + "/5",
                x + MEMBERS_X1, y + MEMBERS_Y1, MEMBERS_X2 - MEMBERS_X1, MEMBERS_Y2 - MEMBERS_Y1,
                0.8f, 0xFF888888);

        if (empire.isFull()) {
            graphics.fill(x + 2, y + CARD_HEIGHT - 12, x + CARD_WIDTH - 2, y + CARD_HEIGHT - 2, 0xAA8B0000);
            graphics.drawString(this.font, Component.literal("FULL"),
                    x + CARD_WIDTH / 2 - 8, y + CARD_HEIGHT - 11, 0xFFFF6666);
        }

        if (adminMode) {
            graphics.fill(x + CARD_WIDTH - 14, y + 2, x + CARD_WIDTH - 2, y + 12, 0xFF8B0000);
            graphics.drawString(this.font, Component.literal("X"), x + CARD_WIDTH - 11, y + 3, 0xFFFF0000);
        }
    }

    private static final int SKIN_MODEL_SIZE = 13;

    private void drawEmperorModel(GuiGraphics graphics, EmpireData empire,
                                  int cardX, int cardY, int mouseX, int mouseY, float partialTick) {
        int x1 = cardX + SKIN_X1;
        int y1 = cardY + SKIN_Y1;
        int x2 = cardX + SKIN_X2;
        int y2 = cardY + SKIN_Y2;

        LivingEntity renderTarget = null;
        if (this.minecraft.level != null) {
            Player p = this.minecraft.level.getPlayerByUUID(empire.getEmperorUUID());
            if (p instanceof LivingEntity living) {
                renderTarget = living;
            }
        }

        if (renderTarget != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    graphics, x1, y1, x2, y2, SKIN_MODEL_SIZE,
                    0.0625f,
                    mouseX,
                    mouseY,
                    renderTarget
            );
        } else {
            graphics.fill(x1, y1, x2, y2, 0xFF1E1E1E);
            graphics.drawString(this.font, Component.literal("?"),
                    x1 + (x2 - x1) / 2 - 2, y1 + (y2 - y1) / 2 - 4, 0xFF555555);
        }
    }

    private void drawScaledCentered(GuiGraphics graphics, String text, int boxX, int boxY,
                                    int boxWidth, int boxHeight, float textScale, int color) {
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(boxX, boxY, 0);
        pose.scale(textScale, textScale, 1f);
        graphics.drawString(this.font, Component.literal(text), 1, 1, color);
        pose.popPose();
    }

    private void drawScrollbar(GuiGraphics graphics) {
        if (maxScroll <= 0) return;

        int trackX = guiLeft + CARD_AREA_X2 + SCROLLBAR_GAP;
        int trackY = guiTop + CARD_AREA_Y1;
        int trackHeight = CARD_AREA_HEIGHT;

        graphics.fill(trackX, trackY, trackX + SCROLLBAR_WIDTH, trackY + trackHeight, 0x33000000);

        float visibleRatio = trackHeight / (float) (trackHeight + maxScroll);
        int thumbHeight = Math.max(16, (int) (trackHeight * visibleRatio));

        float scrollRatio = maxScroll > 0 ? scrollOffset / maxScroll : 0;
        int thumbY = trackY + (int) ((trackHeight - thumbHeight) * scrollRatio);

        graphics.fill(trackX + 1, thumbY, trackX + SCROLLBAR_WIDTH - 1, thumbY + thumbHeight, 0xFF888888);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (float) (scrollY * 16)));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        if (tryHandleSortClick(mouseX, mouseY, SortMode.MEMBER_COUNT,
                guiLeft + SORT_X, guiTop + SORT_FIRST_Y)) return true;
        if (tryHandleSortClick(mouseX, mouseY, SortMode.CLAIMS_COUNT,
                guiLeft + SORT_X, guiTop + SORT_FIRST_Y + SORT_BUTTON_SPACING)) return true;
        if (tryHandleSortClick(mouseX, mouseY, SortMode.DAYS_REGISTERED,
                guiLeft + SORT_X, guiTop + SORT_FIRST_Y + SORT_BUTTON_SPACING * 2)) return true;
        if (tryHandleSortClick(mouseX, mouseY, SortMode.ALPHABETICAL,
                guiLeft + SORT_X, guiTop + SORT_FIRST_Y + SORT_BUTTON_SPACING * 3)) return true;

        if (tryHandleRatioClick(mouseX, mouseY)) return true;

        if (isWithin(mouseX, mouseY,
                guiLeft + FORM_BTN_X1, guiTop + FORM_BTN_Y1, guiLeft + FORM_BTN_X2, guiTop + FORM_BTN_Y2)) {
            handleFormEmpireClick();
            return true;
        }

        int adminX1 = guiLeft + ADMIN_BTN_X1;
        int adminY1 = guiTop + ADMIN_BTN_Y1;
        int adminX2 = guiLeft + ADMIN_BTN_X2;
        int adminY2 = guiTop + ADMIN_BTN_Y2;
        if (isWithin(mouseX, mouseY, adminX1, adminY1, adminX2, adminY2)) {
            if (this.minecraft.player != null && this.minecraft.player.hasPermissions(2)) {
                adminMode = !adminMode;
                return true;
            }
        }

        if (checkCardClicks(mouseX, mouseY, button)) return true;

        int trackX = guiLeft + CARD_AREA_X2 + SCROLLBAR_GAP;
        int trackY = guiTop + CARD_AREA_Y1;
        if (mouseX >= trackX && mouseX <= trackX + SCROLLBAR_WIDTH
                && mouseY >= trackY && mouseY <= trackY + CARD_AREA_HEIGHT) {
            isDraggingScrollbar = true;
            updateScrollFromMouse(mouseY);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void handleFormEmpireClick() {
        var player = this.minecraft.player;
        if (player == null) return;

        EmpireData existing = ClientEmpireCache.getEmpireByPlayer(player.getUUID());
        if (existing != null) {
            player.displayClientMessage(
                    Component.literal("§cYou already belong to an empire (§e" + existing.getName()
                            + "§c) — leave or dissolve it first."),
                    false);
            return;
        }

        this.minecraft.setScreen(new FormAndEditEmpireScreen(null, adminMode));
    }

    private boolean tryHandleSortClick(double mouseX, double mouseY, SortMode mode, int x, int y) {
        if (isWithin(mouseX, mouseY, x, y, x + SIDEBAR_CONTENT_WIDTH, y + SORT_BUTTON_HEIGHT)) {
            sortMode = mode;
            refreshEmpires();
            return true;
        }
        return false;
    }

    private boolean tryHandleRatioClick(double mouseX, double mouseY) {
        for (FlagRatio ratio : FlagRatio.values()) {
            int[] bounds = RATIO_BUTTON_BOUNDS.get(ratio);
            int x1 = guiLeft + bounds[0];
            int y1 = guiTop + bounds[1];
            int x2 = guiLeft + bounds[2];
            int y2 = guiTop + bounds[3];

            if (isWithin(mouseX, mouseY, x1, y1, x2, y2)) {
                ratioFilter = (ratioFilter == ratio) ? null : ratio;
                refreshEmpires();
                return true;
            }
        }
        return false;
    }

    private boolean checkCardClicks(double mouseX, double mouseY, int button) {
        CardLayout layout = computeCardLayout();

        for (int i = 0; i < filteredEmpires.size(); i++) {
            EmpireData empire = filteredEmpires.get(i);
            int col = i % layout.cardsPerRow();
            int row = i / layout.cardsPerRow();
            int cardX = layout.startX() + col * (CARD_WIDTH + CARD_PADDING);
            int cardY = layout.startY() + row * (CARD_HEIGHT + CARD_PADDING);

            if (cardY + CARD_HEIGHT < layout.areaY() || cardY > layout.areaY() + layout.areaHeight()) {
                continue;
            }

            if (isWithin(mouseX, mouseY, cardX, cardY, cardX + CARD_WIDTH, cardY + CARD_HEIGHT)) {

                if (adminMode && isWithin(mouseX, mouseY,
                        cardX + CARD_WIDTH - 14, cardY + 2, cardX + CARD_WIDTH - 2, cardY + 12)) {
                    PacketDistributor.sendToServer(new DeleteEmpireC2SPacket(empire.getName()));
                    return true;
                }

                this.minecraft.setScreen(new FormAndEditEmpireScreen(empire, adminMode));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDraggingScrollbar) {
            updateScrollFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isDraggingScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void updateScrollFromMouse(double mouseY) {
        float relativeY = (float) (mouseY - (guiTop + CARD_AREA_Y1)) / CARD_AREA_HEIGHT;
        scrollOffset = Math.max(0, Math.min(maxScroll, relativeY * maxScroll));
    }

    private static boolean isWithin(double mouseX, double mouseY, int x1, int y1, int x2, int y2) {
        return mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}