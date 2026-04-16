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

    static class MarkedClass implements Marker {}

    static class TestAopFactoryBean extends AopFactoryBean<Marker> {
        public TestAopFactoryBean(Class<?> beanClass) {
            super(beanClass, new TestAopContext());
        }
    }

    static class TestControllerAopBeanPostProcessor extends AopBeanDefinitionRegistryPostProcessor {
        public TestControllerAopBeanPostProcessor() {
            super(new TestAopContext());
        }
    }

    static class TestAopContext extends AopContext<Marker> {

        public TestAopContext() {
            super(Marker.class);
        }

        @Override
        public boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
            return beanDefinition.getBeanClassName().equals(MarkedClass.class.getName());
        }

        @Override
        public Object createProxy(AopFactory aopFactory, EmContext context, AopMetadata metadata) {
            var original = context.getBean(aopFactory.getBeanName() + AopContext.ORIGINAL_BEAN_SUFFIX);
            return new MarkedClass();
        }

        @Override
        public <EX extends Annotation> Class<EX> getExclusionAnnotation() {
            return null;
        }

        @Override
        public Class<? extends AopFactoryBean<?>> factoryBeanClass() {
            return TestAopFactoryBean.class;
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
