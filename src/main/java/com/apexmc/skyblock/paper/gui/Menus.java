package com.apexmc.skyblock.paper.gui;

import com.apexmc.skyblock.core.config.Cfg;
import com.apexmc.skyblock.paper.SkyblockRuntime;

/** Factory for every menu, so commands never construct menus by hand. */
public final class Menus {

    private final SkyblockRuntime runtime;
    private Cfg guiConfig;

    public Menus(SkyblockRuntime runtime, Cfg guiConfig) {
        this.runtime = runtime;
        this.guiConfig = guiConfig;
    }

    public void reload(Cfg guiConfig) {
        this.guiConfig = guiConfig;
    }

    public String title(String key, String fallback) {
        return guiConfig.getString(key, fallback);
    }

    public String filler() {
        return guiConfig.getString("filler-material", "GRAY_STAINED_GLASS_PANE");
    }

    public ProgressionMenu progression() {
        return new ProgressionMenu(runtime, this);
    }

    public IslandLevelsMenu islandLevels() {
        return new IslandLevelsMenu(runtime, this);
    }

    public MasteryMenu mastery() {
        return new MasteryMenu(runtime, this);
    }

    public RolesMenu roles() {
        return new RolesMenu(runtime, this);
    }

    public OmniToolMenu omniTool(String toolId) {
        return new OmniToolMenu(runtime, this, toolId);
    }

    public GeneratorsMenu generators() {
        return new GeneratorsMenu(runtime, this);
    }

    public SpawnersMenu spawners() {
        return new SpawnersMenu(runtime, this);
    }

    public CompanionsMenu companions() {
        return new CompanionsMenu(runtime, this);
    }
}
