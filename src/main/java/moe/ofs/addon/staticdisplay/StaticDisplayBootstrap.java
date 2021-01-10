package moe.ofs.addon.staticdisplay;

import lombok.extern.slf4j.Slf4j;
import moe.ofs.addon.staticdisplay.services.DisplaySpawnControlService;
import moe.ofs.backend.Plugin;
import moe.ofs.backend.handlers.MissionStartObservable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StaticDisplayBootstrap implements Plugin {
    public static final String name = "Static Aircraft Display";
    public static final String desc = "An addon for Project Lava that will spawn a matching static object of " +
            "flyable aircraft parked on apron.";

    private final DisplaySpawnControlService displaySpawnControlService;

    public StaticDisplayBootstrap(DisplaySpawnControlService displaySpawnControlService) {
        this.displaySpawnControlService = displaySpawnControlService;
    }

    @Override
    public void register() {
        log.info("Registering Static Aircraft Display");

        ((MissionStartObservable) (s -> {
            log.info("Triggering setup...");
            displaySpawnControlService.setUp();
        })).register();
    }

    @Override
    public void unregister() {

    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return desc;
    }
}
