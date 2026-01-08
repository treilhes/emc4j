package com.treilhes.emc4j.test.jpa;

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

        repository.save(entity);
    }

}
