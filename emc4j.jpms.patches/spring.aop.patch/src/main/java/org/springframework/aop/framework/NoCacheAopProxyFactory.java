package org.springframework.aop.framework;

import java.lang.reflect.Proxy;

import org.springframework.util.ClassUtils;

/**
 * @inheritDoc
 */
public class NoCacheAopProxyFactory extends DefaultAopProxyFactory {

    public static final NoCacheAopProxyFactory INSTANCE = new NoCacheAopProxyFactory();

    private static final long serialVersionUID = 7930414337282325166L;


    @Override
    public AopProxy createAopProxy(AdvisedSupport config) throws AopConfigException {
        if (config.isOptimize() || config.isProxyTargetClass() || !config.hasUserSuppliedInterfaces()) {
            Class<?> targetClass = config.getTargetClass();
            if (targetClass == null && config.getProxiedInterfaces().length == 0) {
                throw new AopConfigException("TargetSource cannot determine target class: " +
                        "Either an interface or a target is required for proxy creation.");
            }
            if (targetClass == null || targetClass.isInterface() ||
                    Proxy.isProxyClass(targetClass) || ClassUtils.isLambdaClass(targetClass)) {
                return new JdkDynamicAopProxy(config);
            }
            return new NoCacheObjenesisCglibAopProxy(config);
        }
        else {
            return new JdkDynamicAopProxy(config);
        }
    }

}
