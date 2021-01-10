package moe.ofs.addon.staticdisplay.services.impl;

import lombok.extern.slf4j.Slf4j;
import moe.ofs.addon.staticdisplay.model.PlayerSlotChangeAction;
import moe.ofs.addon.staticdisplay.model.SlotChangeProcessEntity;
import moe.ofs.addon.staticdisplay.services.DisplaySpawnControlService;
import moe.ofs.addon.staticdisplay.services.SlotChangeHookInterceptService;
import moe.ofs.addon.staticdisplay.services.StaticObjectRelationManageService;
import moe.ofs.backend.dataservice.parking.ParkingInfoService;
import moe.ofs.backend.dataservice.player.PlayerInfoService;
import moe.ofs.backend.dataservice.slotunit.FlyableUnitService;
import moe.ofs.backend.domain.dcs.poll.PlayerInfo;
import moe.ofs.backend.function.spawncontrol.services.StaticObjectService;
import moe.ofs.backend.handlers.PlayerLeaveServerObservable;
import moe.ofs.backend.object.FlyableUnit;
import moe.ofs.backend.object.ParkingInfo;
import moe.ofs.backend.object.StaticObject;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Service
@Slf4j
public class DisplaySpawnControlServiceImpl implements DisplaySpawnControlService {
    private final StaticObjectRelationManageService manageService;
    private final SlotChangeHookInterceptService interceptService;
    private final FlyableUnitService flyableUnitService;
    private final ParkingInfoService parkingInfoService;
    private final PlayerInfoService playerInfoService;

    private final StaticObjectService staticObjectService;

    private static final List<String> excludedTypeList = Arrays.asList("SA342M", "SA342L",
            "SA342Mistral", "SA342Minigun");

    // slot id as string, runtimeId as string
    private static final Map<String, String> mapSlotStaticId = new HashMap<>();

    // predefined replacement map, used to force spawn with an old lockon models if an matching entry exists
    private static final Map<String, String> replacement = new HashMap<>();

    private final Map<PlayerInfo, PlayerSlotChangeAction> playerSlotMap;

    static {
        replacement.put("FA-18C_hornet", "F/A-18C");
        replacement.put("F-14B", "F-14A");
    }

    public DisplaySpawnControlServiceImpl(StaticObjectRelationManageService manageService,
                                          SlotChangeHookInterceptService interceptService,
                                          FlyableUnitService flyableUnitService,
                                          ParkingInfoService parkingInfoService,
                                          PlayerInfoService playerInfoService,
                                          StaticObjectService staticObjectService) {
        this.manageService = manageService;
        this.interceptService = interceptService;
        this.flyableUnitService = flyableUnitService;
        this.parkingInfoService = parkingInfoService;
        this.playerInfoService = playerInfoService;
        this.staticObjectService = staticObjectService;

        playerSlotMap = new HashMap<>();
    }

    @Override
    public void setUp() {
        log.info("Setting up static display aircraft model for flyable aircraft...");
        flyableUnitService.findAll().forEach(this::spawn);

        interceptService.addProcessor("default-player-slot-change-processor",
                p -> playerInfoService.findByNetId(p.getNetId())
                        .ifPresent(processSlotChange(p)));

        // TODO: what if player just straight disconnects from server? listen for event and modify map entry
        // TODO: better way to do it?
        ((PlayerLeaveServerObservable) playerInfo ->
                flyableUnitService.findByUnitId(playerInfo.getSlot()).ifPresent(this::spawn)).register();
    }

    private Consumer<PlayerInfo> processSlotChange(SlotChangeProcessEntity p) {
        return player -> {
            // check if player has a record in map
            playerSlotMap.computeIfPresent(player, (i, r) -> {
                r.setPrevious(r.getCurrent());
                r.setCurrent(p.getTargetSlot());

                // deal with static display respawn for previous slot
                flyableUnitService.findByUnitId(r.getPrevious()).ifPresent(this::spawn);

                return r;
            });

            // or else if no record, simply put into the map
            if (!playerSlotMap.containsKey(player)) {
                playerSlotMap.put(player, PlayerSlotChangeAction.builder()
                        .previous("")
                        .current(p.getTargetSlot())
                        .player(player)
                        .build());
            }

            log.info("Player <{}>({}) change slot: [{}] => [{}]",
                    player.getName(),
                    player.getUcid(),
                    playerSlotMap.get(player).getPrevious(),
                    playerSlotMap.get(player).getCurrent());
        };
    }

    @Override
    public void teardown() {
        mapSlotStaticId.keySet().forEach(id -> flyableUnitService.findByUnitId(id).ifPresent(this::despawn));
        mapSlotStaticId.clear();
        manageService.resetAssociation();
    }

    @Override
    public void spawn(FlyableUnit flyableUnit) {
        if (flyableUnit == null) {
            return;
        }

        String type;
        String rawType = flyableUnit.getType();
        type = replacement.getOrDefault(rawType, rawType);

        if (!excludedTypeList.contains(type)) {
            String startType = flyableUnit.getStart_type();

            double heading;
            if (startType.equals("TakeOffGround")) {
                heading = flyableUnit.getHeading();
            } else if (startType.equals("TakeOffParking")) {
                Optional<ParkingInfo> optional = parkingInfoService.getParking(flyableUnit.getAirdromeId(),
                        flyableUnit.getParking());

                if (!optional.isPresent()) {
                    System.out.println("flyableUnit.getAirdromeId() = " + flyableUnit.getAirdromeId());
                    System.out.println("flyableUnit.getParking() = " + flyableUnit.getParking());
                    parkingInfoService.getAllParking().stream()
                            .filter(p -> p.getAirdromeName().equals("Nellis AFB"))
                            .findAny().ifPresent(p -> System.out.println(p.getAirdromeName() + ", " +
                            p.getAirdromeId() + ", " + p.getParkingId()));
                }

                heading = parkingInfoService.getParking(flyableUnit.getAirdromeId(), flyableUnit.getParking()).get()
                        .getInitialHeading();
            } else {
                return;
            }

            CompletableFuture<StaticObject> future = staticObjectService.addStaticObject("_StaticDisplay_" + flyableUnit.getUnit_name(),
                    flyableUnit.getX(), flyableUnit.getY(), type,
                    flyableUnit.getLivery_id(), flyableUnit.getOnboard_num(), heading, flyableUnit.getCountry_id());

            future.thenAccept(r -> {
                mapSlotStaticId.put(String.valueOf(flyableUnit.getUnit_id()), String.valueOf(r.getId()));

                manageService.setAssociation(String.valueOf(flyableUnit.getUnit_id()), String.valueOf(r.getId()));

                log.info("Static Object [{}] spawned with runtime id [{}] in livery [{}]",
                        flyableUnit.getUnit_name(), r, flyableUnit.getLivery_id());
            });
        }  // else no spawn
    }

    @Override
    public void despawn(FlyableUnit flyableUnit) {
        String runtimeId = mapSlotStaticId.get(String.valueOf(flyableUnit.getUnit_id()));
        CompletableFuture<Boolean> future = staticObjectService.removeStaticObject(Integer.parseInt(runtimeId));
        future.thenAccept(s -> log.info("Cleaning static display object with runtime id [{}]", runtimeId));
    }
}
