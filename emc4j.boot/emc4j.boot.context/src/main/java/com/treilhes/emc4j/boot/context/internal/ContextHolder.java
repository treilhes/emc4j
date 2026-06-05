package com.treilhes.emc4j.boot.context.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.treilhes.emc4j.boot.api.context.EmContext;
import com.treilhes.emc4j.boot.api.context.EmContextShutdownHooks;
import com.treilhes.emc4j.boot.api.loader.extension.Extension;

public class ContextHolder {
    private Extension extension;
    private EmContext context;
    private ShutdownHooks shutdownHooks;

    public ContextHolder(Extension extension, EmContext context) {
        this.extension = extension;
        this.context = context;
        this.shutdownHooks = new ShutdownHooks();
    }

    public Extension getExtension() {
        return extension;
    }

    public EmContext getContext() {
        return context;
    }

    public EmContextShutdownHooks getShutdownHooks() {
        return shutdownHooks;
    }


    public void executeInheritedHooks() {
        shutdownHooks.inheritedHooks.forEach(hook -> hook.accept(context));
    }


    public void executeHooks() {
        shutdownHooks.hooks.forEach(hook -> hook.accept(context));
    }


    public void inherit(ContextHolder parentHolder) {
        if (parentHolder == null) {
            return;
        }
        var inherited = parentHolder.shutdownHooks.inheritedHooks;
        inherited.forEach(hook -> shutdownHooks.registerInheritedShutdownHook(hook));
    }

    private class ShutdownHooks implements EmContextShutdownHooks {

        private final List<Consumer<EmContext>> hooks = new ArrayList<>();
        private final List<Consumer<EmContext>> inheritedHooks = new ArrayList<>();

        @Override
        public void registerShutdownHook(Consumer<EmContext> hook) {
            if (hook != null) {
                hooks.add(hook);
            }
        }

        @Override
        public void registerInheritedShutdownHook(Consumer<EmContext> hook) {
            if (hook != null) {
                inheritedHooks.add(hook);
            }
        }

    }

}
