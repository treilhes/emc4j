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
/**
 * REST controller that dispatches HTTP requests to the appropriate Extension DispatcherServlet
 * based on a context identifier (the extension id). This controller acts as a dynamic router, allowing requests
 * to be forwarded to different application contexts managed by {@link ContextManager}.
 * <p>
 * The controller exposes endpoints under the REST path prefix defined by {@link EmcPlatform#EXTENSION_REST_PATH_PREFIX}.
 * It supports all major HTTP methods and is primarily used for multi-context or modular Spring Boot applications.
 * </p>
 */
package com.treilhes.emc4j.boot.web.controller.boot;

import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.PATCH;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.DispatcherServlet;

import com.treilhes.emc4j.boot.api.context.ContextManager;
import com.treilhes.emc4j.boot.api.platform.EmcPlatform;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
@RestController
@RequestMapping("/" + EmcPlatform.EXTENSION_REST_PATH_PREFIX)
public class DispatcherRestController {

    /**
     * Request attribute key used to mark requests handled by EMC context routing.
     */
    private static final String EMC_ATTRIBUTE = "EMC-ATTRIBUTE";

    /**
     * Logger for this controller.
     */
    private static final Logger logger = LoggerFactory.getLogger(DispatcherRestController.class);

    /**
     * Context manager responsible for resolving application contexts by UUID.
     */
    private final ContextManager ctxManager;

    /**
     * Default DispatcherServlet instance (may be used for fallback or initial routing).
     */
    DispatcherServlet dso;

    /**
     * Constructs a DispatcherRestController with the given context manager and default DispatcherServlet.
     *
     * @param ctxManager the context manager responsible for resolving application contexts by UUID
     * @param ds the default DispatcherServlet instance (may be used for fallback or initial routing)
     */
    public DispatcherRestController(ContextManager ctxManager, DispatcherServlet ds) {
        super();
        this.ctxManager = ctxManager;
        this.dso = ds;
    }

    /**
     * Handles HTTP requests for a specific context and forwards them to the appropriate DispatcherServlet.
     * This method is mapped to all major HTTP methods and dynamically routes requests based on the context identifier.
     * The http methods supported are GET, POST, PUT, DELETE, and PATCH. It is the responsability of the target DispatcherServlet
     * to handle or not the request method and return the appropriate response.
     * The context identifier is extracted from the URL path and must match an extension id with an UUID format.
     * The remaining path is also extracted and is propagated to the target DispatcherServlet for further processing.
     *
     * @param contextId the unique identifier of the target application context
     * @param remains the remaining path after the context identifier
     * @param request the incoming HTTP servlet request
     * @param response the HTTP servlet response to be populated
     * @throws DispatcherException if an error occurs during request dispatching or context resolution
     */
    @RequestMapping(path = "/{contextId}/{*remains}", method = {GET, POST, PUT, DELETE, PATCH} )
    public void getCall(@PathVariable(name = "contextId") String contextId,
            @PathVariable(name = "remains") String remains, HttpServletRequest request, HttpServletResponse response)
            throws DispatcherException {
        contextCall(contextId, request, response);
    }

    private void contextCall(String contextId, HttpServletRequest request, HttpServletResponse response)
            throws DispatcherException {

        logger.debug("Received request for contextId: {}, path: {}", contextId, request.getRequestURI());

        var id = UUID.fromString(contextId);
        var ctx = ctxManager.get(id);

        if (ctx == null) {
            throw new IllegalStateException("Unable to redirect, context not found for id: " + contextId);
        }

        try {
            request.setAttribute(EMC_ATTRIBUTE, contextId);

            var ds = (DispatcherServlet) ctx.getBean("redirector");
            ds.service(request, response);

        } catch (NoSuchBeanDefinitionException e) {
            throw new IllegalStateException("DispatcherServlet bean not found for context: " + contextId, e);
        } catch (Exception e) {
            if (e.getCause() instanceof RuntimeException) {
                throw new IllegalStateException(e);
            }
            throw new DispatcherException("An error occurred while dispatching the request to context: " + contextId, e);
        }
    }
}