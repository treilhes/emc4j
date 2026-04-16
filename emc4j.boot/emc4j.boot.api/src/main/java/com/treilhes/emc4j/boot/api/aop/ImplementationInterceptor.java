package com.treilhes.emc4j.boot.api.aop;

import java.lang.reflect.InvocationTargetException;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.lang.Nullable;

/**
 * Method interceptor that calls methods on the target object.
 */
public class ImplementationInterceptor implements MethodInterceptor {

    /** The target object to invoke methods on. */
    private final Object base;
    /** The bean class being proxied. */
    private Class<?> beanClass;

    /**
     * Constructs an ImplementationInterceptor for the given target and bean class.
     *
     * @param base      the target object
     * @param beanClass the bean class being proxied
     */
    public ImplementationInterceptor(Object base, Class<?> beanClass) {
        this.base = base;
        this.beanClass = beanClass;
    }

    /**
     * Intercepts method calls and delegates them to the target object.
     *
     * @param invocation the method invocation
     * @return the result of the method call
     * @throws Throwable if the method invocation fails
     */
    @Nullable
    @Override
    public Object invoke(@SuppressWarnings("null") MethodInvocation invocation) throws Throwable {

        var method = invocation.getMethod();
        Object[] arguments = invocation.getArguments();

        try {
            return method.invoke(base, arguments);
        } catch (Exception e) {
            if (e instanceof InvocationTargetException) {
                throw ((InvocationTargetException) e).getTargetException();
            }
            throw e;
        }
    }
}