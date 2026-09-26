package com.apexmc.skyblock.paper.task;

import com.apexmc.skyblock.paper.SkyblockRuntime;
import java.util.logging.Level;

/** Periodic autosave of dirty profiles plus all placed structures (async). */
public final class AutoSaveTask implements Runnable {

    private final SkyblockRuntime runtime;

    public AutoSaveTask(SkyblockRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    public void run() {
        try {
            runtime.profiles().saveDirty();
            runtime.profiles().repository().saveGenerators(runtime.services().generators().all());
            runtime.profiles().repository().saveSpawners(runtime.services().spawners().all());
        } catch (RuntimeException ex) {
            runtime.plugin().getLogger().log(Level.SEVERE, "Autosave failed", ex);
        }
    }
}
