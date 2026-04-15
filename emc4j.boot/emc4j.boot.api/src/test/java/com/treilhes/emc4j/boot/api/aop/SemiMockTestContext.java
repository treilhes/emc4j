package com.treilhes.emc4j.boot.api.aop;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.treilhes.emc4j.boot.api.context.Application;
import com.treilhes.emc4j.boot.api.context.ApplicationInstance;
import com.treilhes.emc4j.boot.api.context.EmContext;
import com.treilhes.emc4j.boot.api.context.MultipleProgressListener;
import com.treilhes.emc4j.boot.api.context.ScopedExecutor;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;

class SemiMockTestContext extends AnnotationConfigApplicationContext implements EmContext {
    public SemiMockTestContext() {
        super();
    }

    @Override
    public void setServletContext(@Nullable ServletContext servletContext) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setServletConfig(@Nullable ServletConfig servletConfig) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public @Nullable ServletConfig getServletConfig() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setNamespace(@Nullable String namespace) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public @Nullable String getNamespace() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setConfigLocation(String configLocation) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setConfigLocations(String... configLocations) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public String @Nullable [] getConfigLocations() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public @Nullable ServletContext getServletContext() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addProgressListener(MultipleProgressListener progressListener) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public String[] getBeanNamesForType(Class<?> cls, Class<?> genericClass) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UUID getUuid() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isExpression(String text) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Object parseExpression(String text, Object rootContext) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isApplicationScope(Class<?> cls) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isApplicationInstanceScope(Class<?> cls) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<Class<?>> getBeanClassesForAnnotation(Class<? extends Annotation> annotationType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> List<Class<T>> getBeanClassesForType(Class<T> cls) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<Class<?>> getRegisteredClasses() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Class<?> getRegisteredClass(String className) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<Class<?>> getDeportedClasses() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T, G, U> Map<String, U> getBeansOfTypeWithGeneric(Class<T> cls, Class<G> generic) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ClassLoader getBeanClassLoader() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T getLocalBean(Class<T> cls) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void destroyBean(Object existingBean) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void destroyScopedBean(String beanName) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public <T> void registerBean(Class<T> class1, Supplier<T> object) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public ScopedExecutor<Application> getApplicationExecutor() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ScopedExecutor<ApplicationInstance> getApplicationInstanceExecutor() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T getLayerBean(Class<?> layerClass, Class<T> cls) {
        // TODO Auto-generated method stub
        return null;
    }
}