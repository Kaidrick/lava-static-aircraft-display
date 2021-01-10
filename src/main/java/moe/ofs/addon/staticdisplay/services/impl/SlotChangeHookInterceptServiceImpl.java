package moe.ofs.addon.staticdisplay.services.impl;

import lombok.extern.slf4j.Slf4j;
import moe.ofs.addon.staticdisplay.model.PlayerSlotChangeAction;
import moe.ofs.addon.staticdisplay.model.SlotChangeProcessEntity;
import moe.ofs.addon.staticdisplay.services.SlotChangeHookInterceptService;
import moe.ofs.addon.staticdisplay.services.StaticObjectRelationManageService;

import moe.ofs.backend.connector.lua.LuaInteract;
import moe.ofs.backend.connector.lua.LuaLoading;
import moe.ofs.backend.connector.lua.LuaQueryEnv;
import moe.ofs.backend.connector.lua.LuaScriptLoader;
import moe.ofs.backend.domain.connector.handlers.scripts.LuaScriptStarter;
import moe.ofs.backend.domain.connector.handlers.scripts.ScriptInjectionTask;
import moe.ofs.backend.function.mizdb.services.impl.LuaStorageInitServiceImpl;
import moe.ofs.backend.hookinterceptor.AbstractHookInterceptorProcessService;
import moe.ofs.backend.hookinterceptor.HookInterceptorDefinition;
import moe.ofs.backend.hookinterceptor.HookType;
import moe.ofs.backend.services.mizdb.SimpleKeyValueStorage;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Service("staticDisplaySlotChangeHookInterceptServiceImpl")
@Slf4j
public class SlotChangeHookInterceptServiceImpl
        extends AbstractHookInterceptorProcessService<SlotChangeProcessEntity, HookInterceptorDefinition>
        implements SlotChangeHookInterceptService, StaticObjectRelationManageService, LuaScriptStarter {

    // key - slot/unit id, value - runtime id
    private final SimpleKeyValueStorage<String> storage;

//    private final Map<String, Consumer<SlotChangeProcessEntity>> processors;



    public SlotChangeHookInterceptServiceImpl() {
        storage = new SimpleKeyValueStorage<>("static-display-slot-change-hook-intercept-service-kv-store",
                LuaQueryEnv.SERVER_CONTROL);

//        processors = new HashMap<>();
    }

    @Override
    public ScriptInjectionTask injectScript() {
        LuaScriptLoader luaScriptLoader = new LuaScriptLoader(getClass());

        return ScriptInjectionTask.builder()
                .scriptIdentName("StaticDisplayHookInterceptService")
                .initializrClass(getClass())
                .dependencyInitializrClass(LuaStorageInitServiceImpl.class)
                .inject(() -> {
                    boolean hooked = createHook(getClass().getName(), HookType.ON_PLAYER_CHANGE_SLOT);

                    HookInterceptorDefinition definition = HookInterceptorDefinition.builder()
                            .name("static-display-slot-change-interceptor")
                            .storage(storage)
                            .predicateFunction(
                                    luaScriptLoader.load("staticdisplay/remove_static_display_object.lua"))
                            .argPostProcessFunction(
                                    luaScriptLoader.load("staticdisplay/slot_change_info_mapper.lua"))
                            .build();

                    return hooked && addDefinition(definition);
                })
                .build();
    }

    @Scheduled(fixedDelay = 200L)
    @LuaLoading
    public void bitter() throws IOException {
        System.out.println("bitter bingo");
        gather(SlotChangeProcessEntity.class);
//        poll(SlotChangeProcessEntity.class)
//                .forEach(slotChangeProcessEntity ->
//                        processors.values().forEach(consumer -> consumer.accept(slotChangeProcessEntity)));
    }

    @Override
    public void setAssociation(String slot, String runtimeId) {
        storage.save(slot, runtimeId);
    }

    @Override
    public void removeAssociation(String slot) {
        storage.delete(slot);
    }

    @Override
    public void resetAssociation() {
        storage.deleteAll();
    }

//    @Override
//    public void addEntityProcessor(String name, Consumer<SlotChangeProcessEntity> consumer) {
//        processors.putIfAbsent(name, consumer);
//    }

//    @Override
//    public void removeEntityProcess(String name) {
//        processors.remove(name);
//    }
}
