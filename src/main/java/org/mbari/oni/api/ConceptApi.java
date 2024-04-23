package org.mbari.oni.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import java.util.List;

@Path("/v1/concept")
public class ConceptApi {

    @GET
    @Path("/")
    public List<String> findAllNames() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

}
