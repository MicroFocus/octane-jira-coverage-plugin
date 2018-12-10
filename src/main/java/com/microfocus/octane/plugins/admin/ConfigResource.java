package com.microfocus.octane.plugins.admin;

import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.sal.api.user.UserManager;
import com.microfocus.octane.plugins.configuration.OctaneConfigurationManager;
import com.microfocus.octane.plugins.components.api.Constants;
import com.microfocus.octane.plugins.configuration.OctaneConfiguration;
import com.microfocus.octane.plugins.rest.OctaneEntityParser;
import com.microfocus.octane.plugins.rest.RestConnector;
import com.microfocus.octane.plugins.rest.entities.OctaneEntity;
import org.apache.commons.lang.StringUtils;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.HashMap;
import java.util.Map;

@Path("/")
@Scanned
public class ConfigResource {

	private static final String PASSWORD_REPLACE = "__secret__password__"; // NON-NLS


	@ComponentImport
	private final UserManager userManager;

	@ComponentImport
	private final PluginSettingsFactory pluginSettingsFactory;

	@ComponentImport
	private final TransactionTemplate transactionTemplate;

	@Inject
	public ConfigResource(UserManager userManager, PluginSettingsFactory pluginSettingsFactory,
						  TransactionTemplate transactionTemplate) {
		this.userManager = userManager;
		this.pluginSettingsFactory = pluginSettingsFactory;
		this.transactionTemplate = transactionTemplate;
	}


	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response get(@Context HttpServletRequest request) {
		String username = userManager.getRemoteUsername(request);
		if (username == null || !userManager.isSystemAdmin(username)) {
			return Response.status(Status.UNAUTHORIZED).build();
		}

		OctaneConfiguration config = OctaneConfigurationManager.loadConfiguration(pluginSettingsFactory);
		config.setClientSecret(PASSWORD_REPLACE);
		return Response.ok(config).build();
	}

	@Path("/test-connection")
	@PUT
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response testConnection(final OctaneConfiguration config, @Context HttpServletRequest request) {
		String username = userManager.getRemoteUsername(request);
		if (username == null || !userManager.isSystemAdmin(username)) {
			return Response.status(Status.UNAUTHORIZED).build();
		}

		String errorMsg = null;
		if (StringUtils.isEmpty(config.getLocation())) {
			errorMsg = "Location URL is required";
		} else if (StringUtils.isEmpty(config.getClientId())) {
			errorMsg = "Client ID is required";
		} else if (StringUtils.isEmpty(config.getClientSecret())) {
			errorMsg = "Client secret is required";
		} else if (StringUtils.isEmpty(config.getOctaneUdf())) {
			errorMsg = "Octane field is required";
		}
		else {
			replacePassword(config);

			OctaneConfiguration.OctaneDetails octaneDetail = null;
			try {
				octaneDetail = OctaneConfigurationManager.parseUiLocation(config.getLocation());
			} catch (IllegalArgumentException ex) {
				errorMsg = ex.getMessage();
			}

			if (errorMsg == null) {
				try {
					RestConnector restConnector = new RestConnector();
					restConnector.setBaseUrl(octaneDetail.getBaseUrl());
					restConnector.setCredentials(config.getClientId(), config.getClientSecret());
					boolean isConnected = restConnector.login();
					if (!isConnected) {
						errorMsg = "Failed to authenticate";
					} else {
						String entityCollectionUrl = String.format(Constants.PUBLIC_API_WORKSPACE_LEVEL_ENTITIES,
								octaneDetail.getSharedspaceId(), octaneDetail.getWorkspaceId(), "");

						Map<String, String> headers = new HashMap<>();
						headers.put(RestConnector.HEADER_ACCEPT, RestConnector.HEADER_APPLICATION_JSON);

						try {
							String entitiesCollectionStr = restConnector.httpGet(entityCollectionUrl, null, headers).getResponseData();

							JSONObject jsonObj = new JSONObject(entitiesCollectionStr);
							OctaneEntity workspaceEntity = OctaneEntityParser.parseEntity(jsonObj);
						} catch (JSONException e) {
							errorMsg = "Incorrect sharedspace id or workspace id";
						}
					}
				} catch (Exception exc) {
					if (exc.getMessage().contains("platform.not_authorized")) {
						errorMsg = "Validate credentials";
					} else if (exc.getMessage().contains("type workspace does not exist")) {
						errorMsg = "Workspace '" + octaneDetail.getWorkspaceId() + "' is not available";
					} else if (exc.getMessage().contains("type shared_space does not exist")) {
						errorMsg = "Sharedspace '" + octaneDetail.getSharedspaceId() + "' is not available";
					} else {
						errorMsg = "Validate that location is correct.";
					}

				}
			}
		}
		if (errorMsg != null) {
			return Response.status(Status.CONFLICT).entity("Failed to connect : " + errorMsg).build();
		} else {
			return Response.ok().build();
		}
	}

	@PUT
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response put(final OctaneConfiguration config, @Context HttpServletRequest request) {
		String username = userManager.getRemoteUsername(request);
		if (username == null || !userManager.isSystemAdmin(username)) {
			return Response.status(Status.UNAUTHORIZED).build();
		}

		replacePassword(config);

		transactionTemplate.execute(new TransactionCallback() {
			public Object doInTransaction() {
				OctaneConfigurationManager.saveConfiguration(pluginSettingsFactory, config);
				return null;
			}
		});

		return Response.ok().build();
	}

	private void replacePassword(OctaneConfiguration config) {
		if (PASSWORD_REPLACE.equals(config.getClientSecret())) {
			OctaneConfiguration tempConfig = OctaneConfigurationManager.loadConfiguration(pluginSettingsFactory);
			config.setClientSecret(tempConfig.getClientSecret());
		}
	}
}
