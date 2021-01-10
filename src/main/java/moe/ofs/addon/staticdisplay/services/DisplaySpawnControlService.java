package moe.ofs.addon.staticdisplay.services;

import moe.ofs.backend.object.FlyableUnit;

public interface DisplaySpawnControlService {
    void setUp();

    void teardown();

    void spawn(FlyableUnit flyableUnit);

    void despawn(FlyableUnit flyableUnit);
}
