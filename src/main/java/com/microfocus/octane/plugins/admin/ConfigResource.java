package com.microfocus.octane.plugins.admin;

import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.sal.api.user.UserManager;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Path("/")
@Scanned
public class ConfigResource {
	String PLUGIN_PREFIX = "com.microfocus.octane.plugins.";
	String OCTANE_LOCATION_KEY = PLUGIN_PREFIX + "octaneUrl";
	String CLIENT_ID_KEY = PLUGIN_PREFIX + "clientId";
	String CLIENT_SECRET_KEY = PLUGIN_PREFIX + "clientSecret";

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
				config.client_secret = (String) settings.get(CLIENT_SECRET_KEY);

				return config;
			}
		})).build();
	}

	@Path("/test-connection")
	@PUT
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response testConnection(final Config data, @Context HttpServletRequest request) {
		return Response.ok().entity(System.currentTimeMillis()).build();
	}


	@PUT
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response put(final Config config, @Context HttpServletRequest request) {
		String username = userManager.getRemoteUsername(request);
		if (username == null || !userManager.isSystemAdmin(username)) {
			return Response.status(Status.UNAUTHORIZED).build();
		}

		transactionTemplate.execute(new TransactionCallback() {
			public Object doInTransaction() {
				PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
				pluginSettings.put(OCTANE_LOCATION_KEY, config.location);
				pluginSettings.put(CLIENT_ID_KEY, config.client_id);
				pluginSettings.put(CLIENT_SECRET_KEY, config.client_secret);
				return null;
			}
		});
		return Response.noContent().build();
	}


	public static final class Config {
		public String location;
		public String client_id;
		public String client_secret;
	}
}
