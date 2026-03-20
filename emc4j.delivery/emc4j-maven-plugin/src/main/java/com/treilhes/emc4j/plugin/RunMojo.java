/*
 * Copyright (c) 2021, 2026, Pascal Treilhes and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Pascal Treilhes nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.treilhes.emc4j.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenFileFilterRequest;
import org.apache.maven.shared.filtering.MavenFilteringException;

import com.treilhes.emc4j.plugin.javaconfig.JavaProcessConfig;
import com.treilhes.emc4j.plugin.util.FsUtil;

@Mojo(name = "run", requiresDependencyResolution = ResolutionScope.TEST, defaultPhase = LifecyclePhase.NONE, threadSafe = true)
public class RunMojo extends Emc4jAbstractMojo {

    private static final String RUN_DIRECTORY = "run";

    private static final String BOOT_CONFIG_FILENAME = "boot.config";

    private static final String DEBUG_OPTION = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=%s,address=0.0.0.0:%s";

    @Component
    private MavenFileFilter mavenFileFilter;
    
    /**
     * The application identifier to run.
     * The application identifier must be contained in one of the provided registry dependencies.
     */
    @Parameter(property = "applicationId", required = true)
    private String applicationId;

    /**
     * If true, the artifacts deployed into the run directory will be deleted first, before being copied.
     * If false, only the configuration file will be updated, and the dependencies will not be copied again.
     */
    @Parameter(property = "clean", defaultValue = "true")
    private boolean clean;

    /**
     * If true, the JVM will be started in debug mode, allowing a debugger to attach.
     * The default value is false.
     */
    @Parameter(property = "debug", defaultValue = "false")
    private boolean debug;
    /**
     * If true, the JVM will wait for a debugger to attach before starting execution.
     * The default value is true.
     */
    @Parameter(property = "debugSuspend", defaultValue = "true")
    private boolean debugSuspend;

    /**
     * The debug port to use when starting the JVM in debug mode.
     * The default port is 8000.
     */
    @Parameter(property = "debugPort", defaultValue = "8000")
    private int debugPort;
    
    @Parameter
    private Registry registry;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            
            if (registry != null) {
                getProfiles().add(0, "emc4jregistry");
            }

            var localRepoPath = getRepositorySession().getLocalRepository().getBasedir().getAbsolutePath();
            
            var cpDirectories = List.of(
                    new File(project.getBuild().getOutputDirectory()),
                    new File(getRunDirectory(), "registry"));

            var javaProcessConfig = initializeJavaProcessConfig();
            javaProcessConfig.addJvmArg("-Demc4j.registry.snapshotsAllowed=true");
            javaProcessConfig.addJvmArg(String.format("-Demc4j.repository.directory=\"%s\"", localRepoPath));
            javaProcessConfig.addAppArg("-a");
            javaProcessConfig.addAppArg(applicationId);

            if (debug) {
                javaProcessConfig.addJvmNonDeferredArg(String.format(DEBUG_OPTION, debugSuspend ? "y" : "n", String.valueOf(debugPort)));
            }

            if (clean) {
                cleanRunDirectory();
                copyDependencies(javaProcessConfig, getRunDirectory());
            }

            generateConfigFile(javaProcessConfig, getRunDirectory(), cpDirectories);
            copyProfileToTarget();

            var to = new File(getRunDirectory(), "registry").toPath();
            Files.createDirectories(to);
            
            if (registry != null && registry.getLocalFolder() != null) { 
                // copy and filter the registry
                var from = registry.getLocalFolder().toPath();
                copyRegistryToTarget(from, to);
                registry.setLocalFolder(to.toFile());
            }

            if (registry != null) {
                var profileContent = generateProfileContent(registry);
                var profileFile = to.resolve("application-emc4jregistry.yaml");
                Files.writeString(profileFile, profileContent);
            }

            run(javaProcessConfig);

            System.out.println();
        } catch (Exception e) {
            getLog().error("Error during run", e);
            throw new MojoExecutionException("Error during run", e);
        }

    }

    public void run(JavaProcessConfig jcfg) throws MojoExecutionException {
        try {

            List<String> command = new ArrayList<>();

            command.add(jcfg.getJavaBin().getAbsolutePath());
            command.addAll(jcfg.getJvmNonDeferredArgs());
            command.add("@" + BOOT_CONFIG_FILENAME);

            String cmd = "Running command: %s, working directory: %s";
            getLog().info(String.format(cmd, String.join(" ", command), getRunDirectory().getAbsolutePath()));

            // Start process
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            pb.directory(getRunDirectory());

            Process process = pb.start();

            // Forward process output to Maven log
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    getLog().info("[java] " + line);
                }
            }

            // Wait for completion (blocking)
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new MojoExecutionException("Java process exited with code " + exitCode);
            }

        } catch (Exception e) {
            throw new MojoExecutionException("Failed to run Java command", e);
        }
    }

    private void copyProfileToTarget() throws IOException {
        if (getProfileFiles() != null && !getProfileFiles().isEmpty()) {
            for (File profileFile : getProfileFiles()) {
                File targetProfile = new File(getRunDirectory(), profileFile.getName());
                Files.copy(profileFile.toPath(), targetProfile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
    
    private void copyRegistryToTarget(Path from, Path to) throws IOException, MojoExecutionException {
        if (registry != null && registry.getLocalFolder() != null) {
            var excluded = Pattern.compile("emc4j-registry\\.xml");
            var registryFile = from.resolve("emc4j-registry.xml");
            var targetRegistryFile = to.resolve("emc4j-registry.xml");
            FsUtil.copyDirectory(from, to, excluded);
            filter(registryFile, targetRegistryFile);
        }
    }

    protected void cleanRunDirectory() throws IOException {
        File runFolder = getRunDirectory();
        if (runFolder.exists()) {
            FsUtil.deleteDirectory(runFolder);
        }
    }

    public File getRunDirectory() {
        return new File(getOutputDirectory(), RUN_DIRECTORY);
    }



    public void generateConfigFile(JavaProcessConfig jcfg, File targetFolder, List<File> cpDirectories) throws Exception {

        File configFile = new File(targetFolder, BOOT_CONFIG_FILENAME);

        String patchFormat = "--patch-module %s=%s";
        String addReadFormat = "--add-reads %s";
        String addOpensFormat = "--add-opens %s";
        String addExportsFormat = "--add-exports %s";

        StringBuilder sb = new StringBuilder();

        sb.append("--module-path ./mp").append("\n");

        List<String> cpItems = new ArrayList<>();
        cpItems.add("./cp/*");

        if (cpDirectories != null) {
            cpDirectories.forEach(f -> cpItems.add(f.getAbsolutePath()));
        }

        // here the claspath definition will use file separator to separate items
        // this is only done in the run mojo for testing purposes, as the mvnTargetClassFolder is only used in the run mojo, and not in the launch mojo
        // it must be kept as is in order to assure the cross platform compatibility of the plugin, as the file separator is different on Windows and Unix systems
        String mergedCp = String.join(File.pathSeparator, cpItems);

        sb.append("--class-path ").append(mergedCp).append("\n");

        for (String addRead : jcfg.getAddReads()) {
            sb.append(String.format(addReadFormat, addRead)).append("\n");
        }
        for (String addOpen : jcfg.getAddOpens()) {
            sb.append(String.format(addOpensFormat, addOpen)).append("\n");
        }
        for (String addExport : jcfg.getAddExports()) {
            sb.append(String.format(addExportsFormat, addExport)).append("\n");
        }

        for (Entry<String, List<File>> patch : jcfg.getPatchModules().entrySet()) {
            for (File f : patch.getValue()) {
                String patchPath = targetFolder.getAbsolutePath() + File.separator + "patch" + File.separator + f.getName();
                sb.append(String.format(patchFormat, patch.getKey(), patchPath)).append("\n");
            }
        }

        for (String arg : jcfg.getJvmArgs()) {
            sb.append(arg).append("\n");
        }

        sb.append("-m").append(" ").append(jcfg.getMainModule()).append("/").append(jcfg.getMainClass()).append("\n");

        for (String arg : jcfg.getAppArgs()) {
            sb.append(arg).append("\n");
        }

        Files.writeString(configFile.toPath(), sb);
    }
    
    private String generateProfileContent(Registry registry) {
        
        /*
         emc4j:
            registry:
              snapshotsAllowed: true
              defaults:
                emc4j_registry:
                  groupId: com.treilhes.emc4j
                  artifactId: emc4j.it.samples.registry
                  mandatory: true
         */
        StringBuilder sb = new StringBuilder();
        sb.append("emc4j:").append("\n");
        sb.append("  registry:").append("\n");
        sb.append("    snapshotsAllowed: true").append("\n");
        sb.append("    defaults:").append("\n");
        sb.append("      emc4jregistry:").append("\n");
        sb.append("        mandatory: true").append("\n");
        sb.append("        groupId: ").append(registry.getGroupId()).append("\n");
        sb.append("        artifactId: ").append(registry.getArtifactId()).append("\n");
        
        if (registry.getVersion() != null) {
            sb.append("        version: ").append(registry.getVersion()).append("\n");
        }
        
        if (registry.getLocalFolder() != null) {
            sb.append("        localFolder: ").append(registry.getLocalFolder().getAbsolutePath()).append("\n");
        }
        
        return sb.toString();
    }
    
    private void filter(Path source, Path target) throws MojoExecutionException {

        MavenFileFilterRequest request = new MavenFileFilterRequest();
        request.setFrom(source.toFile());
        request.setTo(target.toFile());

        // VERY IMPORTANT
        request.setMavenProject(project);
        request.setMavenSession(getSession());

        // enable filtering
        request.setFiltering(true);

        try {
            mavenFileFilter.copyFile(request);
        } catch (MavenFilteringException e) {
            throw new MojoExecutionException("Filtering failed", e);
        }
    }
    
    public static class Registry {
        String groupId; 
        String artifactId; 
        String version;
        File localFolder;
        public String getGroupId() {
            return groupId;
        }
        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }
        public String getArtifactId() {
            return artifactId;
        }
        public void setArtifactId(String artifactId) {
            this.artifactId = artifactId;
        }
        public String getVersion() {
            return version;
        }
        public void setVersion(String version) {
            this.version = version;
        }
        public File getLocalFolder() {
            return localFolder;
        }
        public void setLocalFolder(File localFolder) {
            this.localFolder = localFolder;
        }
    }
}