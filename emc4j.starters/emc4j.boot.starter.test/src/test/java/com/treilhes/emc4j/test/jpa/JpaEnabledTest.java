/*
 * Copyright (c) 2021, 2026, Pascal Treilhes and/or its affiliates.
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
package com.treilhes.emc4j.test.jpa;



import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.test.context.jdbc.Sql;

import com.treilhes.emc4j.test.Emc4jTest;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Emc4jTest(
        enableJpa = true,
        webEnvironment = WebEnvironment.DEFINED_PORT,
        properties = {
                "spring.h2.console.enabled=true"//,
                //"spring.jpa.open-in-view=false",
                //"spring.jpa.hibernate.ddl-auto=create-drop"
        },
        classes = {
                JpaEnabledTest.Entiy.class,
                JpaEnabledTest.Repository.class
        }
)
@Sql(statements = {
        //"create table EntityTable (id bigint not null, name varchar(255), primary key (id))",
        "insert into XX_Entity_Table (id, name) values (100, 'Preloaded')"
})
class JpaEnabledTest {

    @Entity(name = "XX_EntityTable")
    static class Entiy {
        @Id
        private Long id;
        private String name;

        public Long getId() {
            return id;
        }
        public void setId(Long id) {
            this.id = id;
        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
    }

    static interface Repository extends JpaRepository<Entiy, Long> {
    }

    @Autowired
    Repository repository;

    @Test
    void test() {
        var entity = new Entiy();
        entity.setId(1L);
        entity.setName("Test");
        entity = repository.save(entity);
        assertNotNull(entity);
    }

}
