/*
 *     Copyright 2018 EntIT Software LLC, a Micro Focus company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package com.microfocus.octane.plugins.views;

import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.microfocus.octane.plugins.components.api.OctaneRestService;
import com.microfocus.octane.plugins.configuration.OctaneConfigurationManager;
import com.microfocus.octane.plugins.descriptors.OctaneEntityTypeDescriptor;
import com.microfocus.octane.plugins.descriptors.OctaneEntityTypeManager;
import com.microfocus.octane.plugins.rest.entities.MapBasedObject;
import com.microfocus.octane.plugins.rest.entities.OctaneEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/coverage")
@Scanned
public class CoverageResource {

    private static final Logger log = LoggerFactory.getLogger(CoverageResource.class);

    @ComponentImport
    private final UserManager userManager;

    private final OctaneRestService octaneRestService;

    @Inject
    public CoverageResource(UserManager userManager, OctaneRestService octaneRestService) {
        this.userManager = userManager;
        this.octaneRestService = octaneRestService;
    }

    @GET
    public Response getCoverageWithFilter(@Context HttpServletRequest request,
                                          @QueryParam("filter-date") String filterDate,
                                          @QueryParam("entity-id") String entityId,
                                          @QueryParam("entity-path") String entityPath,
                                          @QueryParam("entity-type") String entityType,
                                          @QueryParam("workspace-id") long workspaceId) {
        UserProfile userProfile = userManager.getRemoteUser(request);
        if (userProfile == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        OctaneConfigurationManager.getInstance().setUserFilter(userProfile.getUsername(), filterDate);//persist filter

        OctaneEntity entity = new OctaneEntity(entityType);
        entity.put("id", entityId);
        entity.put("path", entityPath);

        OctaneEntityTypeDescriptor typeDescriptor = OctaneEntityTypeManager.getByTypeName(entityType);
        List<MapBasedObject> groups = CoverageUiHelper.getCoverageGroups(octaneRestService, entity, typeDescriptor, workspaceId, filterDate);
        return Response.ok(groups).build();
    }
}
