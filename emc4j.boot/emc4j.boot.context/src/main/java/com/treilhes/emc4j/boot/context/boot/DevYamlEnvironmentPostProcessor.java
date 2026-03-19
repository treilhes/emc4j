package com.treilhes.emc4j.boot.context.boot;

import org.springframework.boot.SpringApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.ApplicationListener;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class DevYamlEnvironmentPostProcessor implements EnvironmentPostProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(DevYamlEnvironmentPostProcessor.class);

    private static final String FILE_NAME = ".dev.yaml";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {

        if (!environment.matchesProfiles("devlookup")) {
            logger.info("Enable devlookup profile to lookup for .dev.yaml configuration");
            return;
        }
        
        logger.info("Looking for .dev.yaml in working directory then parent folders until root is reached or .dev.yaml is found");
        
        File file = findFileUpwards(new File("."));

        if (file != null && file.exists()) {
            
            logger.info(".dev.yaml found : {}", file.getAbsolutePath());
            
            try {
                YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
                List<PropertySource<?>> yamlTestProperties = loader.load("devYaml", new FileSystemResource(file));

                // Add with high priority
                for (PropertySource<?> ps : yamlTestProperties) {
                    environment.getPropertySources().addFirst(ps);
                }

                System.out.println("Loaded config from: " + file.getAbsolutePath());

            } catch (IOException e) {
                throw new RuntimeException("Failed to load .dev.yaml", e);
            }
        }
    }

    private File findFileUpwards(File dir) {
        
        dir = dir.getAbsoluteFile();
        
        while (dir != null) {
            File candidate = new File(dir, FILE_NAME);
            if (candidate.exists()) {
                return candidate;
            } else {
                logger.debug(".dev.yaml not found in : {}", dir.getAbsolutePath());
            }
            dir = dir.getParentFile();
        }
        
        return null;
    }

    /**
     * Convenience helper to register this EnvironmentPostProcessor on a SpringApplication instance.
     * Usage: DevYamlEnvironmentPostProcessor.register(application);
     */
    public static void register(SpringApplication application) {
        application.addListeners((ApplicationListener<ApplicationEnvironmentPreparedEvent>) event -> {
            DevYamlEnvironmentPostProcessor p = new DevYamlEnvironmentPostProcessor();
            p.postProcessEnvironment(event.getEnvironment(), event.getSpringApplication());
        });
    }
}