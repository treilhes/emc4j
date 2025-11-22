package com.treilhes.emc4j.registry.mapper.impl;

import java.io.InputStream;
import java.io.OutputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.treilhes.emc4j.registry.mapper.InvalidRegistryException;
import com.treilhes.emc4j.registry.mapper.Mapper;
import com.treilhes.emc4j.registry.model.Registry;

public class JsonMapper implements Mapper {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Registry from(InputStream stream) {
        try {
            return mapper.readValue(stream, Registry.class);
        } catch (Exception e) {
            throw new InvalidRegistryException(e);
        }
    }

    @Override
    public void to(Registry registry, OutputStream output) {
        try {
            mapper.writeValue(output, registry);
        } catch (Exception e) {
            throw new InvalidRegistryException(e);
        }
    }
}
