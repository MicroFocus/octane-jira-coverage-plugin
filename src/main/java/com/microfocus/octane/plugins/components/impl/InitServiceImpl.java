package com.microfocus.octane.plugins.components.impl;

import com.atlassian.jira.cluster.ClusterMessagingService;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.microfocus.octane.plugins.components.api.InitService;
import com.microfocus.octane.plugins.configuration.ConfigurationManager;

import javax.inject.Inject;
import javax.inject.Named;

@ExportAsService({InitService.class})
@Named("initService")
public class InitServiceImpl implements InitService {

    @ComponentImport
    private final PluginSettingsFactory pluginSettingsFactory;

    @ComponentImport
    ClusterMessagingService clusterMessagingService;

    @Inject
    public InitServiceImpl(final PluginSettingsFactory pluginSettingsFactory, ClusterMessagingService clusterMessagingService) {
        this.pluginSettingsFactory = pluginSettingsFactory;
        ConfigurationManager.getInstance().init(pluginSettingsFactory, clusterMessagingService);
    }

}
