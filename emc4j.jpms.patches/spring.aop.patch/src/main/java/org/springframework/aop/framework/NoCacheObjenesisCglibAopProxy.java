package org.springframework.aop.framework;

import org.springframework.cglib.proxy.Enhancer;

public class NoCacheObjenesisCglibAopProxy extends ObjenesisCglibAopProxy {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public NoCacheObjenesisCglibAopProxy(AdvisedSupport config) throws AopConfigException {
        super(config);
    }

    @Override
    protected Enhancer createEnhancer() {
        var enhancer = super.createEnhancer();
        enhancer.setUseCache(false);
        return enhancer;
    }

}
