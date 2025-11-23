package com.treilhes.emc4j.boot.maven.api;

import java.util.List;
import java.util.Set;

import com.treilhes.emc4j.boot.api.maven.Artifact;
import com.treilhes.emc4j.boot.api.maven.Repository;

public interface SearchService {

    void cancelSearch();

    Set<Artifact> search(String query, List<Repository> repositories);

}