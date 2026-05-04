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
package com.treilhes.emc4j.boot.main.command;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;

import com.treilhes.emc4j.boot.api.splash.ContextLoadingAdapter;
import com.treilhes.emc4j.boot.context.boot.BootContext;
import com.treilhes.emc4j.boot.main.config.BootHandler;
import com.treilhes.emc4j.boot.main.util.MessageBox;
import com.treilhes.emc4j.boot.main.util.MessageBoxMessage;
import com.treilhes.emc4j.boot.platform.internal.DefaultFolders;
import com.treilhes.emc4j.boot.splash.impl.BootSplashScreen;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Command(subcommands = { RunFxmlCommand.class })
public class StartCommand implements Runnable, MessageBox.Delegate<MessageBoxMessage> {

    private static final Logger logger = LoggerFactory.getLogger(StartCommand.class);

    private static MessageBox<MessageBoxMessage> messageBox;

    @Option(names = { "--root", "-r" }, defaultValue = "./target", description = "Extensions download folder")
    private Path root;

    @Option(names = { "--app", "-a" }, description = "target application uuid")
    private UUID targetApplication;

    private BootHandler bootHandler;

    @Spec
    private CommandSpec spec;

    @Override
    public void run() {

        String[] originalArgs = spec.commandLine().getParseResult().originalArgs().toArray(new String[0]);
        List<String> arguments = Arrays.asList(originalArgs);

        try {
            if (!lockMessageBox(this, targetApplication, arguments)) {
                logger.warn("An instance is already running forwarding execution to existing instance");
                return;
            }
        } catch (IOException e) {
            logger.error("Unable to initialize the message box", e);
        }

        if (bootHandler == null) {
            var splash = BootSplashScreen.defaultSplashScreen();
            var step = splash.asSubSteps(1).get(0);
            var contextAdapter = new ContextLoadingAdapter(step);

            var context = BootContext.create(null, WebApplicationType.SERVLET, originalArgs, c -> {
                c.addApplicationListener(contextAdapter);
                c.addBeanFactoryPostProcessor(contextAdapter);
            });
            bootHandler = context.getBean(BootHandler.class);
        }

        bootHandler.boot(targetApplication, arguments);

    }

    /*
     * Private (requestStartGeneric)
     */

    private static synchronized boolean lockMessageBox(MessageBox.Delegate<MessageBoxMessage> delegate, UUID targetApp,
            List<String> arguments) throws IOException {
        assert messageBox == null;

        var messageBoxFolder = DefaultFolders.getMessageBoxFolder();

        try {
            Files.createDirectories(messageBoxFolder.toPath());
        } catch (FileAlreadyExistsException x) {
            // Fine
        }

        final boolean result;
        messageBox = new MessageBox<>(messageBoxFolder, MessageBoxMessage.class, 1000 /* ms */);

        // Fix End
        if (messageBox.grab(delegate)) {
            result = true;
        } else {
            result = false;

            final MessageBoxMessage message = new MessageBoxMessage(targetApp, arguments);
            try {
                messageBox.sendMessage(message);
            } catch (InterruptedException x) {
                throw new IOException(x);
            }
        }

        return result;
    }

    @Override
    public void messageBoxDidGetMessage(MessageBoxMessage message) {
        try {
            UUID targetApplication = message.getTargetApplication();
            bootHandler.boot(targetApplication, message.getArguments());
        } catch (Exception e) {
            logger.error("Unable to execute the message {} the application", message, e);
        }
    }

    @Override
    public void messageBoxDidCatchException(Exception ex) {
        logger.error("Received message but something failed", ex);
    }

}