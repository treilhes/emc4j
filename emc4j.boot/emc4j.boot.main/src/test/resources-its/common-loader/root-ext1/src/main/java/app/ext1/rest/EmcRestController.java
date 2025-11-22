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
package app.ext1.rest;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
//import jakarta.transaction.Transactional;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.ext1.model.EmcModel;
import app.ext1.repository.EmcRepository;
import app.ext1.service.EmcDataService;
import app.ext1.test.EmcAspectTest;
import jakarta.validation.Valid;

@RestController
@RequestMapping(path = "/models", produces = MediaType.APPLICATION_JSON_VALUE)
@SuppressWarnings("exports")
public class EmcRestController {

    private final EmcRepository repository;
    private final EmcDataService service;
    private final EmcAspectTest emcAspectTest;

    public EmcRestController(EmcRepository repository, EmcDataService service, EmcAspectTest emcAspectTest) {
        super();
        this.repository = repository;
        this.service = service;
        this.emcAspectTest = emcAspectTest;
    }

    @GetMapping()
    public ResponseEntity<List<EmcModel>> list() {
        var obj = repository.findAll();
        return ResponseEntity.ok(obj);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmcModel> read(@PathVariable(name = "id") Long id) {
        var obj = repository.findById(id).orElse(null);
        return ResponseEntity.ok(obj);
    }

    @PostMapping()
    public ResponseEntity<EmcModel> create(@RequestBody @Valid EmcModel model) {
        var obj = repository.save(model);
        return ResponseEntity.ok(obj);
    }

    @PutMapping()
    public ResponseEntity<EmcModel> update(@RequestBody EmcModel model) {
        var obj = repository.save(model);
        return ResponseEntity.ok(obj);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<EmcModel> delete(@PathVariable(name = "id") Long id) {
        repository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping()
    public ResponseEntity<EmcModel> deleteAll() {
        repository.deleteAll();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/derivation")
    public ResponseEntity<EmcModel> derivation() {
        var obj = repository.getTopByOrderByDataAsc();
        return ResponseEntity.ok(obj);
    }

    @GetMapping("/query")
    public ResponseEntity<List<EmcModel>> query() {
        var obj = repository.customQuery();
        return ResponseEntity.ok(obj);
    }

    @PostMapping("/transaction_rollback_in_service")
    @Transactional
    public ResponseEntity<EmcModel> transaction_rollback_in_service(@RequestBody EmcModel model) {
        service.transactionFailed(model);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/transaction_rollback_in_repository")
    @Transactional
    public ResponseEntity<EmcModel> transaction_rollback_in_repository(@RequestBody EmcModel model) throws Exception {
        repository.transactionFailed(model);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/throw_controler_advice_handled_exception")
    public ResponseEntity<?> throwControlerAdviceHandledException() throws Exception {
        throw new Exception("controllerAdviceHandledException");
    }

    @GetMapping("/testing_aspects_are_applied")
    public ResponseEntity<String> testing_aspects_are_applied() throws Exception {
        return ResponseEntity.ok(emcAspectTest.execute());
    }

    @PostMapping("/testing_validation_is_applied")
    public ResponseEntity<String> testing_validation_is_applied(@RequestBody @Valid EmcModel model) throws Exception {
        return ResponseEntity.ok().build();
    }
}
