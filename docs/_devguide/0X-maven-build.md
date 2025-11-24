---
title: "Maven Build"
permalink: /devguide/maven-build/
excerpt: "Notes about the maven build"
last_modified_at: 2021-06-07T08:48:05-04:00
toc: true
---

## Maven Builds

### Project environment

Maven musn't use an embedded  installation, you need to configure an external maven installation in your IDE or use the command line.

### Project build

Building the project is pretty straightforward, a simple `mvn clean install` will build the project.

### Delivery build

 To generate the project javadoc and source artifacts execute `clean install -P delivery`
 