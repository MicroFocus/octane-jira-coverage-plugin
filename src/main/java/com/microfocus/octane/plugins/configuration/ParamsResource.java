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

package com.microfocus.octane.plugins.configuration;

import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/")
@Scanned
public class ParamsResource {

    private static final Logger log = LoggerFactory.getLogger(ParamsResource.class);

    @ComponentImport
    private final UserManager userManager;

    @Inject
    public ParamsResource(UserManager userManager) {
        this.userManager = userManager;
    }

    @GET
    @Path("show-debug")
    //http://localhost:2990/jira/rest/octane-params/1.0/show-debug?visible=true
    public Response showDebugInfo(@Context HttpServletRequest request,
                                  @QueryParam("visible") boolean visible) {
        UserProfile userProfile = userManager.getRemoteUser(request);
        if (userProfile == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        ConfigurationManager.getInstance().setUserParameter(userProfile.getUsername(), ConfigurationManager.SHOW_DEBUG_PARAMETER, visible);
        return Response.ok("Done show-debug : " + visible).build();
    }

    @GET
    @Path("clear-config/{version}")
    //http://localhost:2990/jira/rest/octane-params/1.0/clear-config/1
    public Response clearConfiguration(@Context HttpServletRequest request, @PathParam("version") Integer version) {
        UserProfile userProfile = userManager.getRemoteUser(request);
        if (userProfile == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        String result = "no version";
        if (version != null) {
            result = ConfigurationManager.getInstance().clearConfiguration(version);
        }
        return Response.ok(result).build();
    }
}
