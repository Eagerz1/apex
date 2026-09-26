package com.apexmc.skyblock.testsupport;

import com.apexmc.skyblock.core.data.InMemoryRepository;
import com.apexmc.skyblock.core.data.PlayerProfile;

/** Repository stand-in whose writes always fail, used to test error handling. */
public final class FailingRepository extends com.apexmc.skyblock.core.data.DelegatingRepository {

    public FailingRepository() {
        super(new InMemoryRepository());
    }

    @Override
    public void savePlayer(PlayerProfile profile) {
        throw new IllegalStateException("database down");
    }
}
