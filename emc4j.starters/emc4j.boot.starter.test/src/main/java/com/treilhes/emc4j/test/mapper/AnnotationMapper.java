package com.treilhes.emc4j.test.mapper;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.core.annotation.MergedAnnotation;

import com.treilhes.emc4j.boot.api.loader.extension.SealedExtension;
import com.treilhes.emc4j.test.Emc4jApplicationContext;
import com.treilhes.emc4j.test.Emc4jCoreContext;
import com.treilhes.emc4j.test.Emc4jDefault;
import com.treilhes.emc4j.test.Emc4jExtensionContext;
import com.treilhes.emc4j.test.Emc4jNestedExtensionContext;
import com.treilhes.emc4j.test.Emc4jTest;

public class AnnotationMapper {

    public static BootConfig map(Emc4jTest annotation) {
        if (annotation == null) {
            return null;
        }

        BootConfig bootConfig = new BootConfig();

        bootConfig.setUuid(com.treilhes.emc4j.boot.api.loader.extension.Extension.BOOT_ID);
        bootConfig.getProperties().addAll(List.of(annotation.properties()));
        bootConfig.setWebEnvironment(annotation.webEnvironment());
        bootConfig.setEnableJpa(annotation.enableJpa());
        bootConfig.setEnableAop(annotation.enableAop());
        bootConfig.setLoadDefaultScopes(annotation.loadDefaultScopes());
        bootConfig.setLocalClasses(List.of(annotation.classes()));

        var context = map(annotation.context());

        if (context != null) {
            bootConfig.getSealedExtensions().put(context.getUuid(), context);
        }

        var defaultConfig = map(annotation.defaultConfig());

        if (defaultConfig != null) {
            bootConfig.setDefaultConfig(defaultConfig);
        }

        return bootConfig;
    }

    public static BootConfig map(MergedAnnotation<Emc4jTest> annotation) {
        if (annotation == null) {
            return null;
        }

        BootConfig bootConfig = new BootConfig();

        bootConfig.setUuid(com.treilhes.emc4j.boot.api.loader.extension.Extension.BOOT_ID);
        bootConfig.setWebEnvironment(
                annotation.getValue("webEnvironment", WebEnvironment.class).orElse(WebEnvironment.NONE));
        bootConfig.setEnableJpa(annotation.getValue("enableJpa", Boolean.class).orElse(false));
        bootConfig.setEnableAop(annotation.getValue("enableAop", Boolean.class).orElse(false));
        bootConfig.setLoadDefaultScopes(annotation.getValue("loadDefaultScopes", Boolean.class).orElse(false));

        bootConfig.getProperties()
                .addAll(List.of(annotation.getValue("properties", String[].class).orElse(new String[0])));
        bootConfig.getLocalClasses()
                .addAll(List.of((Class<?>[]) annotation.getValue("classes", Class[].class).orElse(new Class[0])));

        var ctxAnnotation = annotation.getValue("context", Emc4jCoreContext.class).get();

        var context = map(ctxAnnotation);

        if (context != null) {
            bootConfig.getSealedExtensions().put(context.getUuid(), context);
        }

        var defaultConfig = map(annotation.getValue("defaultConfig", Emc4jDefault.class).get());

        if (defaultConfig != null) {
            bootConfig.setDefaultConfig(defaultConfig);
        }

        return bootConfig;
    }

    public static BootConfig map(Emc4jDefault annotation) {
        if (annotation == null) {
            return null;
        }
        if (!AnnotationChecker.isSet(annotation)) {
            return null;
        }

        BootConfig bootConfig = new BootConfig();

        bootConfig.setUuid(com.treilhes.emc4j.boot.api.loader.extension.Extension.BOOT_ID);
        bootConfig.getProperties().addAll(List.of(annotation.properties()));
        bootConfig.getLocalClasses().addAll(List.of(annotation.classes()));
        bootConfig.setWebEnvironment(annotation.webEnvironment());
        bootConfig.setEnableJpa(annotation.enableJpa());
        bootConfig.setEnableAop(annotation.enableAop());
        bootConfig.setLoadDefaultScopes(annotation.loadDefaultScopes());

        var context = map(annotation.context());

        if (context != null) {
            bootConfig.getSealedExtensions().put(context.getUuid(), context);
        }

        return bootConfig;
    }

    public static ContextConfig map(Emc4jCoreContext annotation) {
        if (annotation == null) {
            return null;
        }
        if (!AnnotationChecker.isSet(annotation)) {
            return null;
        }
        ContextConfig contextConfig = new ContextConfig();

        contextConfig.setUuid(SealedExtension.ROOT_ID);
        contextConfig.getProperties().addAll(List.of(annotation.properties()));
        contextConfig.getLocalClasses().addAll(List.of(annotation.classes()));
        contextConfig.getOpenExtensions().putAll(map(annotation.extensions()));
        contextConfig.getSealedExtensions().putAll(map(annotation.applications()));

        return contextConfig;
    }

    public static ContextConfig map(Emc4jApplicationContext annotation) {
        if (annotation == null) {
            return null;
        }
        if (!AnnotationChecker.isSet(annotation)) {
            return null;
        }
        ContextConfig contextConfig = new ContextConfig();

        if (annotation.uuid() != null && !annotation.uuid().isBlank()) {
            contextConfig.setUuid(UUID.fromString(annotation.uuid()));
        }
        contextConfig.getProperties().addAll(List.of(annotation.properties()));
        contextConfig.getLocalClasses().addAll(List.of(annotation.classes()));
        contextConfig.getOpenExtensions().putAll(map(annotation.extensions()));

        return contextConfig;
    }

    public static ContextConfig map(Emc4jExtensionContext annotation) {
        if (annotation == null) {
            return null;
        }
        if (!AnnotationChecker.isSet(annotation)) {
            return null;
        }
        ContextConfig contextConfig = new ContextConfig();

        if (annotation.uuid() != null && !annotation.uuid().isBlank()) {
            contextConfig.setUuid(UUID.fromString(annotation.uuid()));
        }
        contextConfig.getProperties().addAll(List.of(annotation.properties()));
        contextConfig.getLocalClasses().addAll(List.of(annotation.classes()));
        contextConfig.getExportedClasses().addAll(List.of(annotation.exportedClasses()));
        contextConfig.getOpenExtensions().putAll(map(annotation.extensions()));

        return contextConfig;
    }

    public static ContextConfig map(Emc4jNestedExtensionContext annotation) {
        if (annotation == null) {
            return null;
        }
        if (!AnnotationChecker.isSet(annotation)) {
            return null;
        }
        ContextConfig contextConfig = new ContextConfig();

        if (annotation.uuid() != null && !annotation.uuid().isBlank()) {
            contextConfig.setUuid(UUID.fromString(annotation.uuid()));
        }
        contextConfig.getProperties().addAll(List.of(annotation.properties()));
        contextConfig.getLocalClasses().addAll(List.of(annotation.classes()));
        contextConfig.getExportedClasses().addAll(List.of(annotation.exportedClasses()));

        return contextConfig;
    }

    public static Map<UUID, ContextConfig> map(Emc4jApplicationContext[] applications) {
        if (applications == null || applications.length == 0) {
            return new HashMap<UUID, ContextConfig>();
        }

        return Arrays.stream(applications).map(AnnotationMapper::map).filter(Predicate.not(Objects::isNull))
                .collect(Collectors.toMap(ContextConfig::getUuid, c -> c, (m1, m2) -> m2, HashMap::new ));
    }

    public static Map<UUID, ContextConfig> map(Emc4jExtensionContext[] extensions) {
        if (extensions == null || extensions.length == 0) {
            return new HashMap<UUID, ContextConfig>();
        }

        return Arrays.stream(extensions).map(AnnotationMapper::map).filter(Predicate.not(Objects::isNull))
                .collect(Collectors.toMap(ContextConfig::getUuid, c -> c, (m1, m2) -> m2, HashMap::new ));
    }

    public static Map<UUID, ContextConfig> map(Emc4jNestedExtensionContext[] extensions) {
        if (extensions == null || extensions.length == 0) {
            return new HashMap<UUID, ContextConfig>();
        }

        return Arrays.stream(extensions).map(AnnotationMapper::map).filter(Predicate.not(Objects::isNull))
                .collect(Collectors.toMap(ContextConfig::getUuid, c -> c, (m1, m2) -> m2, HashMap::new ));
    }

}
