package org.mbari.oni.api;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.mbari.oni.jpa.services.ConceptNameService;
import org.mbari.oni.jpa.services.ConceptService;

import java.util.List;

@Path("/v1/concept")
public class ConceptApi {

    @Inject
    ConceptService conceptService;

    @Inject
    ConceptNameService conceptNameService;

    @GET
    @Path("/")
    @Transactional
    public List<String> findAllNames() {
        return conceptNameService.findAllNamesAsStrings();
    }

}
