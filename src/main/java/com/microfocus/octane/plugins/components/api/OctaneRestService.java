package com.microfocus.octane.plugins.components.api;

import com.microfocus.octane.plugins.rest.entities.OctaneEntityCollection;
import com.microfocus.octane.plugins.rest.entities.groups.GroupEntityCollection;
import com.microfocus.octane.plugins.rest.query.QueryPhrase;

import java.util.Collection;

public interface OctaneRestService {

    GroupEntityCollection getCoverageForApplicationModule(String applicationModulePath);

    OctaneEntityCollection getEntitiesByCondition(String collectionName, Collection<QueryPhrase> conditions, Collection<String> fields);

    void reloadConfiguration();
}