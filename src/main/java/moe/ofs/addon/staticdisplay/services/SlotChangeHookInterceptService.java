package moe.ofs.addon.staticdisplay.services;

import moe.ofs.addon.staticdisplay.model.SlotChangeProcessEntity;
import moe.ofs.backend.hookinterceptor.HookInterceptorDefinition;
import moe.ofs.backend.hookinterceptor.HookInterceptorProcessService;

import java.util.function.Consumer;

public interface SlotChangeHookInterceptService
        extends HookInterceptorProcessService<SlotChangeProcessEntity, HookInterceptorDefinition> {
//    void addEntityProcessor(String name, Consumer<SlotChangeProcessEntity> consumer);

//    void removeEntityProcess(String name);
}
