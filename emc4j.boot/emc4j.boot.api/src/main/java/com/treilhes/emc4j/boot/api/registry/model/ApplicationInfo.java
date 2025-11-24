/*
 * Copyright (c) 2021, 2025, Pascal Treilhes and/or its affiliates.
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
package com.treilhes.emc4j.boot.api.registry.model;

import java.net.URL;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents metadata and state information about an application in the EMC4J registry.
 * <p>
 * This class holds details such as unique identifier, images, internationalization resources,
 * title, description, changelog, versioning, and installation status.
 * </p>
 */
public class ApplicationInfo {

    /**
     * The unique identifier for the application.
     */
    private UUID uuid;
    /**
     * The URL of the splash image for the application.
     */
    private URL splash;
    /**
     * The URL of the main image for the application.
     */
    private URL image;
    /**
     * The URL for internationalization resources.
     */
    private URL i18n;
    /**
     * The title of the application.
     */
    private String title;
    /**
     * The description or text for the application.
     */
    private String text;
    /**
     * The changelog for the application.
     */
    private String changelog;
    /**
     * The current version of the application.
     */
    private String version;
    /**
     * The next available version of the application.
     */
    private String nextVersion;
    /**
     * Indicates whether the application is installed.
     */
    private boolean installed;

    /**
     * Constructs an empty ApplicationInfo instance.
     */
    public ApplicationInfo() {
    }

    /**
     * Returns the unique identifier for the application.
     *
     * @return the UUID of the application
     */
    public UUID getUuid() {
        return uuid;
    }

    /**
     * Sets the unique identifier for the application.
     *
     * @param uuid the UUID to set
     */
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    /**
     * Returns the splash image URL for the application.
     *
     * @return the splash image URL
     */
    public URL getSplash() {
        return splash;
    }

    /**
     * Sets the splash image URL for the application.
     *
     * @param splash the splash image URL to set
     */
    public void setSplash(URL splash) {
        this.splash = splash;
    }

    /**
     * Returns the main image URL for the application.
     *
     * @return the main image URL
     */
    public URL getImage() {
        return image;
    }

    /**
     * Sets the main image URL for the application.
     *
     * @param image the main image URL to set
     */
    public void setImage(URL image) {
        this.image = image;
    }

    /**
     * Returns the internationalization resource URL for the application.
     *
     * @return the i18n resource URL
     */
    public URL getI18n() {
        return i18n;
    }

    /**
     * Sets the internationalization resource URL for the application.
     *
     * @param i18n the i18n resource URL to set
     */
    public void setI18n(URL i18n) {
        this.i18n = i18n;
    }

    /**
     * Returns the title of the application.
     *
     * @return the application title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title of the application.
     *
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Returns the description or text for the application.
     *
     * @return the application description
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the description or text for the application.
     *
     * @param text the description to set
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Returns the changelog for the application.
     *
     * @return the changelog
     */
    public String getChangelog() {
        return changelog;
    }

    /**
     * Sets the changelog for the application.
     *
     * @param changelog the changelog to set
     */
    public void setChangelog(String changelog) {
        this.changelog = changelog;
    }

    /**
     * Returns the current version of the application.
     *
     * @return the current version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the current version of the application.
     *
     * @param version the version to set
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Returns the next available version of the application.
     *
     * @return the next version
     */
    public String getNextVersion() {
        return nextVersion;
    }

    /**
     * Sets the next available version of the application.
     *
     * @param nextVersion the next version to set
     */
    public void setNextVersion(String nextVersion) {
        this.nextVersion = nextVersion;
    }

    /**
     * Returns whether the application is installed.
     *
     * @return true if installed, false otherwise
     */
    public boolean isInstalled() {
        return installed;
    }

    /**
     * Sets the installation status of the application.
     *
     * @param installed true if installed, false otherwise
     */
    public void setInstalled(boolean installed) {
        this.installed = installed;
    }

    /**
     * Computes the hash code for this ApplicationInfo based on its UUID.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

    /**
     * Compares this ApplicationInfo to another for equality based on UUID.
     *
     * @param obj the object to compare
     * @return true if the UUIDs are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        var other = (ApplicationInfo) obj;
        return Objects.equals(uuid, other.uuid);
    }

}
