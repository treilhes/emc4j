package com.treilhes.emc4j.boot.api.aop;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.annotation.Annotation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;

import com.treilhes.emc4j.boot.api.context.EmContext;

@ExtendWith(MockitoExtension.class)
class AopContextTest {

    static interface Marker{}
    static @interface TestAnnotation {}
    static class TestAnnotationMetadata extends AopMetadata<TestAnnotation, Marker> {

        public TestAnnotationMetadata(Class<TestAnnotation> annotationClass, Class<Marker> markerClass,
                Class<?> beanClass) {
            super(annotationClass, markerClass, beanClass);
        }

        @Override
        protected void loadMetadata(TestAnnotation annotation) {
        }

    }
    static class TestAopFactoryBean extends AopFactoryBean<Marker, TestAnnotationMetadata> {

        protected TestAopFactoryBean(Class<?> beanClass) {
            super(beanClass, new TestAopContext());
        }
    }

    @TestAnnotation
    static class MarkedClass implements Marker {}

    static class TestAopContext extends AopContext<Marker, TestAnnotation, TestAnnotationMetadata> {

        public TestAopContext() {
            super(Marker.class, TestAnnotation.class);
        }

        @Override
        public boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
            return beanDefinition.getBeanClassName().equals(MarkedClass.class.getName());
        }

        @Override
        public Class<? extends AopFactoryBean<Marker, TestAnnotationMetadata>> factoryBeanClass() {
            return TestAopFactoryBean.class;
        }

        @Override
        public TestAnnotationMetadata loadMetadata(Class<?> clazz) {
            return new TestAnnotationMetadata(getContexAnnotationClass(), getMarkerClass(), clazz);
        }

        @Override
        public Marker createTarget(AopFactory aopFactory, EmContext context, TestAnnotationMetadata metadata) {
            return new Marker() {

            };
        }

        @Override
        public <EX extends Annotation> Class<EX> getExclusionAnnotation() {
            return null;
        }

    }
    static class TestControllerAopBeanPostProcessor extends AopBeanDefinitionRegistryPostProcessor {
        public TestControllerAopBeanPostProcessor() {
            super(new TestAopContext());
        }
    }

    @Spy
    SemiMockTestContext testContext;

    @Test
    void test() {

        Mockito.doReturn(MarkedClass.class).when(testContext).getRegisteredClass(MarkedClass.class.getName());

        // Create context manually (lightweight, no full Spring Boot)
        testContext.register(TestControllerAopBeanPostProcessor.class, MarkedClass.class);
        testContext.refresh();

        // Get bean
        var markedInstance = testContext.getBean(MarkedClass.class);

        assertNotNull(markedInstance);

        testContext.close();

    }

}
