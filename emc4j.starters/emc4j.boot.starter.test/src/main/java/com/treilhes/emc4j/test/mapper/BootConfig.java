package com.treilhes.emc4j.test.mapper;

import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

public class BootConfig extends ContextConfig {
    WebEnvironment webEnvironment = WebEnvironment.NONE;
    boolean enableJpa = false;
    boolean enableAop = false;
    boolean loadDefaultScopes = true;

    BootConfig defaultConfig = null;
    public WebEnvironment getWebEnvironment() {
        return webEnvironment;
    }
    public void setWebEnvironment(WebEnvironment webEnvironment) {
        this.webEnvironment = webEnvironment;
    }
    public boolean isEnableJpa() {
        return enableJpa;
    }
    public void setEnableJpa(boolean enableJpa) {
        this.enableJpa = enableJpa;
    }
    public boolean isEnableAop() {
        return enableAop;
    }
    public void setEnableAop(boolean enableAop) {
        this.enableAop = enableAop;
    }
    public boolean isLoadDefaultScopes() {
        return loadDefaultScopes;
    }
    public void setLoadDefaultScopes(boolean loadDefaultScopes) {
        this.loadDefaultScopes = loadDefaultScopes;
    }
    public BootConfig getDefaultConfig() {
        return defaultConfig;
    }
    public void setDefaultConfig(BootConfig defaultConfig) {
        this.defaultConfig = defaultConfig;
    }

}
