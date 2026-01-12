package com.treilhes.emc4j.test.mapper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

/**
 * Merges BootConfig objects, giving precedence to the target over the source.
 * Something to keep in mind is that collections are merged by adding all elements from
 * the source to the target. This means that there can be duplicates in the resulting collections.
 * Also we reuse the same instances of collections and objects which me mean that modifying
 * the source after merging can affect the target.
 * As those objects are usually created for configuration purposes and only used internally
 * i don't think this will cause issues but it's something to keep in mind.
 */
public class BootConfigMerger {

    public static BootConfig merge(BootConfig config) {
        var defaultConfig = config.getDefaultConfig();
        while ( defaultConfig != null ) {
            config = merge( config, defaultConfig );
            defaultConfig = defaultConfig.getDefaultConfig();
        }
        return config;
    }

    public static BootConfig merge(BootConfig target, BootConfig source) {
        if ( target == null ) {
            return source;
        }
        if ( source == null ) {
            return target;
        }

        target.setEnableAop(target.isEnableAop() || source.isEnableAop());
        target.setEnableJpa(target.isEnableJpa() || source.isEnableJpa());

        WebEnvironment env = WebEnvironment.NONE;
        if ( target.getWebEnvironment() != WebEnvironment.NONE ) {
            env = target.getWebEnvironment();
        } else if ( source.getWebEnvironment() != WebEnvironment.NONE ) {
            env = source.getWebEnvironment();
        }

        target.setWebEnvironment(env);

        target.setLoadDefaultScopes(target.isLoadDefaultScopes() || source.isLoadDefaultScopes());

        merge( (ContextConfig) target, (ContextConfig) source );

        return target;

    }

    public static ContextConfig merge(ContextConfig target, ContextConfig source) {
        if ( target == null ) {
            return source;
        }
        if ( source == null ) {
            return target;
        }

        target.getProperties().addAll( source.getProperties() );
        target.getLocalClasses().addAll( source.getLocalClasses() );
        target.getExportedClasses().addAll( source.getExportedClasses() );

        var openExtensions = merge( target.getOpenExtensions(), source.getOpenExtensions() );
        target.setOpenExtensions(openExtensions);

        var sealedExtensions = merge( target.getSealedExtensions(), source.getSealedExtensions() );
        target.setSealedExtensions(sealedExtensions);

        return target;

    }

    public static Map<UUID, ContextConfig> merge(Map<UUID, ContextConfig> target, Map<UUID, ContextConfig> source) {

        var map = new HashMap<UUID, ContextConfig>();

        if ( target == null ) {
            map.putAll( source );
            return map;
        }
        if ( source == null ) {
            map.putAll( target );
            return map;
        }

        map.putAll( target );
        for ( var entry : source.entrySet() ) {
            var key = entry.getKey();
            var sourceValue = entry.getValue();
            if ( target.containsKey( key ) ) {
                var targetValue = target.get( key );
                merge( targetValue, sourceValue );
            } else {
                map.put( key, sourceValue );
            }
        }

        return map;

    }
}
