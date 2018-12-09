package com.microfocus.octane.plugins.admin;

import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.sal.api.user.UserManager;
import com.microfocus.octane.plugins.components.api.Constants;
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
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;

@Path("/")
@Scanned
public class ConfigResource {
	private static final String PLUGIN_PREFIX = "com.microfocus.octane.plugins.";
	private static final String OCTANE_LOCATION_KEY = PLUGIN_PREFIX + "octaneUrl";
	private static final String CLIENT_ID_KEY = PLUGIN_PREFIX + "clientId";
	private static final String CLIENT_SECRET_KEY = PLUGIN_PREFIX + "clientSecret";

	private static final String PARAM_SHARED_SPACE = "p"; // NON-NLS
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

		return Response.ok(transactionTemplate.execute(new TransactionCallback() {
			public Object doInTransaction() {
				PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
				Config config = new Config();
				config.location = (String) settings.get(OCTANE_LOCATION_KEY);
				config.client_id = (String) settings.get(CLIENT_ID_KEY);
				config.client_secret = PASSWORD_REPLACE;

				return config;
			}
		})).build();
	}

	@Path("/test-connection")
	@PUT
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response testConnection(final Config config, @Context HttpServletRequest request) {
		String username = userManager.getRemoteUsername(request);
		if (username == null || !userManager.isSystemAdmin(username)) {
			return Response.status(Status.UNAUTHORIZED).build();
		}

		String errorMsg = null;
		if (StringUtils.isEmpty(config.location)) {
			errorMsg = "Location URL is required";
		} else if (StringUtils.isEmpty(config.client_id)) {
			errorMsg = "Client ID is required";
		} else if (StringUtils.isEmpty(config.client_secret)) {
			errorMsg = "Client secret is required";
		} else {
			replacePassword(config);

			OctaneDetails octaneDetail = null;
			try {
				octaneDetail = parseUiLocation(config.location);
			} catch (IllegalArgumentException ex) {
				errorMsg = ex.getMessage();
			}

			if (errorMsg == null) {
				try {
					RestConnector restConnector = new RestConnector();
					restConnector.setBaseUrl(octaneDetail.getBaseUrl());
					restConnector.setCredentials(config.client_id, config.client_secret);
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
	public Response put(final Config config, @Context HttpServletRequest request) {
		String username = userManager.getRemoteUsername(request);
		if (username == null || !userManager.isSystemAdmin(username)) {
			return Response.status(Status.UNAUTHORIZED).build();
		}

		replacePassword(config);

		transactionTemplate.execute(new TransactionCallback() {
			public Object doInTransaction() {
				PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
				pluginSettings.put(OCTANE_LOCATION_KEY, config.location);
				pluginSettings.put(CLIENT_ID_KEY, config.client_id);
				pluginSettings.put(CLIENT_SECRET_KEY, config.client_secret);
				return null;
			}
		});
		return Response.ok().build();
	}

	private void replacePassword(Config config) {
		if (PASSWORD_REPLACE.equals(config.client_secret)) {
			PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
			config.client_secret = (String) settings.get(CLIENT_SECRET_KEY);
		}
	}

	private static OctaneDetails parseUiLocation(String uiLocation) {
		OctaneDetails details = new OctaneDetails();
		String errorMsg = null;
		try {
			URL url = new URL(uiLocation);
			int contextPos = uiLocation.toLowerCase().indexOf("/ui");
			if (contextPos < 0) {
				errorMsg = "Location url is missing '/ui' part ";
			} else {

				details.setBaseUrl(uiLocation.substring(0, contextPos));
				Map<String, List<String>> queries = splitQuery(url);

				if (queries.containsKey(PARAM_SHARED_SPACE)) {
					List<String> sharedSpaceParamValue = queries.get(PARAM_SHARED_SPACE);
					if (sharedSpaceParamValue != null && !sharedSpaceParamValue.isEmpty()) {
						String[] sharedSpaceAndWorkspace = sharedSpaceParamValue.get(0).split("/");
						if (sharedSpaceAndWorkspace.length == 2) {
							details.setSharedspaceId(sharedSpaceAndWorkspace[0]);
							details.setWorkspaceId(sharedSpaceAndWorkspace[1]);
						} else {
							errorMsg = "Location url has invalid sharedspace/workspace part";
						}

					}
				} else {
					errorMsg = "Location url is missing sharedspace id";
				}
			}

		} catch (Exception e) {
			errorMsg = "Location contains invalid URL ";
		}
		if (errorMsg != null) {
			throw new IllegalArgumentException(errorMsg);
		}
		return details;
	}


	private static Map<String, List<String>> splitQuery(URL url) throws UnsupportedEncodingException {
		final Map<String, List<String>> query_pairs = new LinkedHashMap<String, List<String>>();
		final String[] pairs = url.getQuery().split("&");
		for (String pair : pairs) {
			final int idx = pair.indexOf("=");
			final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
			if (!query_pairs.containsKey(key)) {
				query_pairs.put(key, new LinkedList<String>());
			}
			final String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : null;
			query_pairs.get(key).add(value);
		}
		return query_pairs;
	}

	public static final class Config {
		public String location;
		public String client_id;
		public String client_secret;
	}

	public static final class OctaneDetails {
		private String baseUrl;
		private String sharedspaceId;
		private String workspaceId;

		public String getBaseUrl() {
			return baseUrl;
		}

		public void setBaseUrl(String baseUrl) {
			this.baseUrl = baseUrl;
		}

		public String getSharedspaceId() {
			return sharedspaceId;
		}

		public void setSharedspaceId(String sharedspaceId) {
			this.sharedspaceId = sharedspaceId;
		}

		public String getWorkspaceId() {
			return workspaceId;
		}

		public void setWorkspaceId(String workspaceId) {
			this.workspaceId = workspaceId;
		}
	}

}
