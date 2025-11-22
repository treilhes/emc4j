/*
 * Copyright (c) 2021, 2024, Pascal Treilhes and/or its affiliates.
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
package com.treilhes.emc4j.boot.context.scope;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.treilhes.emc4j.boot.api.context.Application;
import com.treilhes.emc4j.boot.api.context.annotation.ApplicationSingleton;
import com.treilhes.emc4j.boot.context.impl.EmContextImpl;
import com.treilhes.emc4j.boot.context.scope.ApplicationScope;
import com.treilhes.emc4j.boot.context.scope.NullScopeException;

class ApplicationScopeTestIT {

    @BeforeEach
    public void init() {
        EmContextImpl.applicationScope.clear();
    }

    @Test
    void application_scope_must_throw_if_application_scope_does_not_exists() {

        var context = new EmContextImpl(UUID.randomUUID());
        context.register(new Class[] {AppBean.class, SomeBean.class});
        context.refresh();

        // no AppBean created > no ApplicationInstance > no ApplicationInstanceScope

        assertNull(EmContextImpl.applicationScope.getCurrentScope());
        assertThrows(NullScopeException.class, () -> context.getBean(SomeBean.class));

    }

    @Test
    void scope_instance_should_be_created_only_if_no_active_scope() {
        var context = new EmContextImpl(UUID.randomUUID());
        context.register(new Class[] {AppBean.class});
        context.refresh();

        // no active scope
        assertNull(EmContextImpl.applicationScope.getActiveScope());
        assertNull(EmContextImpl.applicationScope.getCurrentScope());

        // a new scope bean must be created and a new associated scope
        var scope1Bean = context.getBean(AppBean.class);

        // check scope bean is the new active scope
        assertEquals(scope1Bean, EmContextImpl.applicationScope.getActiveScope().getScopedObject());

        var scope1 = EmContextImpl.applicationScope.getCurrentScope();

        assertNotNull(scope1);

        assertEquals(scope1, EmContextImpl.applicationScope.getScope(scope1Bean));

        assertEquals(scope1.getId().toString(),
                context.getBeanFactory().getRegisteredScope(ApplicationScope.SCOPE_NAME).getConversationId());

        // check requesting by type returns the currently scope bean
        assertEquals(scope1Bean, context.getBean(AppBean.class));

        // no active scope
        EmContextImpl.applicationScope.unbindScope();

        // a new scope bean must be created and a new associated scope
        var scope2Bean = context.getBean(AppBean.class);

        var scope2 = EmContextImpl.applicationScope.getCurrentScope();

        assertNotNull(scope2);

        assertEquals(scope2, EmContextImpl.applicationScope.getScope(scope2Bean));

        assertEquals(scope2.getId().toString(),
                context.getBeanFactory().getRegisteredScope(ApplicationScope.SCOPE_NAME).getConversationId());

        // check app2bean is the new active scope
        assertEquals(scope2Bean, EmContextImpl.applicationScope.getActiveScope().getScopedObject());
        // check requesting by type returns the currently scoped application
        assertEquals(scope2Bean, context.getBean(AppBean.class));
    }

    @Test
    void scoped_bean_instance_must_be_created_once_per_scope_for_all_factories() {

        var context = new EmContextImpl(UUID.randomUUID());
        context.register(new Class[] {AppBean.class, SomeBean.class});
        context.refresh();

        // a new scope bean must be created and a new associated scope
        var scope1Bean = context.getBean(AppBean.class);
        // a new scope bean must be created and associated with scope1Bean scope
        var pBean = context.getBean(SomeBean.class);

        // no active scope
        EmContextImpl.applicationScope.unbindScope();

        // a new scope bean must be created and a new associated scope
        var scope2Bean = context.getBean(AppBean.class);
        // a new somebean must be created and associated with scope2Bean scope
        var pBean2 = context.getBean(SomeBean.class);

        assertNotEquals(pBean, pBean2);

        // ensure we have only 2 scopes
        assertEquals(2, EmContextImpl.applicationScope.getAvailableScopes().size());

        EmContextImpl.applicationScope.setCurrentScope(scope1Bean);
        var pBeanLatest = context.getBean(SomeBean.class);

        // ensure we get the right instance of bean in current scope
        assertEquals(pBean, pBeanLatest);

        EmContextImpl.applicationScope.setCurrentScope(scope2Bean);
        pBeanLatest = context.getBean(SomeBean.class);

        // ensure we get the right instance of pBean in scope2Bean scope
        assertEquals(pBean2, pBeanLatest);
    }

    @Test
    void scoped_bean_instance_must_use_scoped_dependencies() {

        var context = new EmContextImpl(UUID.randomUUID());
        context.register(new Class[] {AppBean.class, ParentBean.class, ChildBean.class, CompositeBean.class});
        context.refresh();

        var scope1Bean = context.getBean(AppBean.class);
        var pBean = context.getBean(ParentBean.class);
        var cBean = context.getBean(ChildBean.class);
        var compBean = context.getBean(CompositeBean.class);

        EmContextImpl.applicationScope.unbindScope();

        var scope2Bean = context.getBean(AppBean.class);
        var pBean2 = context.getBean(ParentBean.class);
        var cBean2 = context.getBean(ChildBean.class);
        var compBean2 = context.getBean(CompositeBean.class);

        assertNotEquals(pBean, pBean2);
        assertNotEquals(cBean, cBean2);
        assertNotEquals(compBean, compBean2);
        assertNotEquals(pBean, compBean2.getParentBean());
        assertNotEquals(cBean, compBean2.getChildBean());

        EmContextImpl.applicationScope.setCurrentScope(scope1Bean);
        var pBeanLatest = context.getBean(ParentBean.class);
        var cBeanLatest = context.getBean(ChildBean.class);
        var compBeanLatest = context.getBean(CompositeBean.class);

        assertEquals(pBean, pBeanLatest);
        assertEquals(cBean, cBeanLatest);
        assertEquals(compBean, compBeanLatest);
        assertEquals(pBean, compBeanLatest.getParentBean());
        assertEquals(cBean, compBeanLatest.getChildBean());

        EmContextImpl.applicationScope.setCurrentScope(scope2Bean);
        pBeanLatest = context.getBean(ParentBean.class);
        cBeanLatest = context.getBean(ChildBean.class);
        compBeanLatest = context.getBean(CompositeBean.class);

        assertEquals(pBean2, pBeanLatest);
        assertEquals(cBean2, cBeanLatest);
        assertEquals(compBean2, compBeanLatest);
        assertEquals(pBean2, compBeanLatest.getParentBean());
        assertEquals(cBean2, compBeanLatest.getChildBean());
    }

    @Test
    void with_two_scope_ensure_running_with_another_scope_does_not_change_the_current_one() {
        var context = new EmContextImpl(UUID.randomUUID());
        context.register(new Class[] {AppBean.class});
        context.refresh();

        // no active scope
        assertNull(EmContextImpl.applicationScope.getActiveScope());
        // a new appbean must be created and a new associated scope
        var scope1Bean = context.getBean(AppBean.class);
        // no active scope
        EmContextImpl.applicationScope.unbindScope();
        // a new appbean must be created and a new associated scope
        var scope2Bean = context.getBean(AppBean.class);

        var scope1 = EmContextImpl.applicationScope.getScope(scope1Bean);
        var scope2 = EmContextImpl.applicationScope.getScope(scope2Bean);

        // executed with scope: scope1
        EmContextImpl.applicationScope.executeRunnable(() -> {
            assertEquals(scope1, EmContextImpl.applicationScope.getActiveScope());
            assertEquals(scope1Bean, context.getBean(AppBean.class));
            assertEquals(scope2, EmContextImpl.applicationScope.getCurrentScope());
        }, scope1);

        // executed with scope: scope1
        EmContextImpl.applicationScope.executeSupplier(() -> {
            assertEquals(scope1, EmContextImpl.applicationScope.getActiveScope());
            assertEquals(scope1Bean, context.getBean(AppBean.class));
            assertEquals(scope2, EmContextImpl.applicationScope.getCurrentScope());
            return null;
        }, scope1);

        // check scope did not change
        assertEquals(scope2, EmContextImpl.applicationScope.getActiveScope());
        assertEquals(scope2, EmContextImpl.applicationScope.getCurrentScope());

        // change the scope
        EmContextImpl.applicationScope.setCurrentScope(scope1Bean);

        // executed with scope: scope2
        EmContextImpl.applicationScope.executeRunnable(() -> {
            assertEquals(scope2, EmContextImpl.applicationScope.getActiveScope());
            assertEquals(scope2Bean, context.getBean(AppBean.class));
            assertEquals(scope1, EmContextImpl.applicationScope.getCurrentScope());
        }, scope2);

        // executed with scope: scope2
        EmContextImpl.applicationScope.executeSupplier(() -> {
            assertEquals(scope2, EmContextImpl.applicationScope.getActiveScope());
            assertEquals(scope2Bean, context.getBean(AppBean.class));
            assertEquals(scope1, EmContextImpl.applicationScope.getCurrentScope());
            return null;
        }, scope2);

        // check scope did not change
        assertEquals(scope1, EmContextImpl.applicationScope.getActiveScope());
        assertEquals(scope1, EmContextImpl.applicationScope.getCurrentScope());
    }

    @ApplicationSingleton
    public static class AppBean implements Application {
    }

    @ApplicationSingleton
    public static class SomeBean {
    }

    @ApplicationSingleton
    public static class ParentBean {
    }

    @ApplicationSingleton
    public static class ChildBean {
    }

    @ApplicationSingleton
    public static class CompositeBean {
        private final ParentBean parentBean;
        private final ChildBean childBean;

        public CompositeBean(ParentBean parentBean, ChildBean childBean) {
            this.parentBean = parentBean;
            this.childBean = childBean;
        }

        public ParentBean getParentBean() {
            return parentBean;
        }

        public ChildBean getChildBean() {
            return childBean;
        }
    }
}
