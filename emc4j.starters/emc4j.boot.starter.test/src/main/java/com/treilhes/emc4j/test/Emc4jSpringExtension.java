package com.treilhes.emc4j.test;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.treilhes.emc4j.boot.api.context.EmContext;

/**
 * SpringExtension that excludes EmContext parameters from being resolved by the original SpringExtension.
 */
public class Emc4jSpringExtension extends SpringExtension {

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        if (EmContext.class.isAssignableFrom(parameterContext.getParameter().getType())) {
            return false;
        }
        return super.supportsParameter(parameterContext, extensionContext);
    }

}
