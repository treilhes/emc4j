package com.treilhes.emc4j.boot.api.context;

import java.util.function.Consumer;

public interface EmContextShutdownHooks {
    /**
     * Registers a shutdown hook to be executed when the context is shutting down.
     * @param hook
     */
    void registerShutdownHook(Consumer<EmContext> hook);
    /**
     * Registers a shutdown hook to be executed when the context is shutting down, and inherited by child contexts.
     * @param hook
     */
    void registerInheritedShutdownHook(Consumer<EmContext> hook);

}
