/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package svt.application;

import java.util.Set;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.Json;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.validation.Validator;
import jakarta.validation.ConstraintViolation;
import org.bson.Document;
import org.bson.types.ObjectId;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

@Path("/crew")
@ApplicationScoped
public class CrewService {

    @Inject MongoDatabase db;
    @Inject Validator validator;
    private JsonArray getViolations(CrewMember crewMember) {
        Set<ConstraintViolation<CrewMember>> violations = validator.validate(
                crewMember);

        JsonArrayBuilder messages = Json.createArrayBuilder();

        for (ConstraintViolation<CrewMember> v : violations) {
            messages.add(v.getMessage());
        }

        return messages.build();
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Successfully deleted crew member."),
        @APIResponse(responseCode = "400", description = "Invalid object id."),
        @APIResponse(responseCode = "404", description = "Crew member object id was not found.") })
    @Operation(summary = "Delete a crew member from the database.")
    public Response remove(
        @Parameter(description = "Object id of the crew member to delete.", required = true)
        @PathParam("id") String id) {

        ObjectId oid;

        try {
            oid = new ObjectId(id);
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("[\"Invalid object id!\"]").build();
        }

        MongoCollection<Document> crew = db.getCollection("Crew");

        Document query = new Document("_id", oid);

        DeleteResult deleteResult = crew.deleteOne(query);

        if (deleteResult.getDeletedCount() == 0) {
            return Response.status(Response.Status.NOT_FOUND).entity("[\"_id was not found!\"]").build();
        }

        return Response.status(Response.Status.OK).entity(query.toJson()).build();
    }
}
