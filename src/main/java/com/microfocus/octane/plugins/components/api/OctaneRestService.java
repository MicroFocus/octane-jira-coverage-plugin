package com.microfocus.octane.plugins.components.api;

import com.microfocus.octane.plugins.rest.entities.OctaneEntity;
import com.microfocus.octane.plugins.rest.entities.OctaneEntityCollection;
import com.microfocus.octane.plugins.rest.entities.groups.GroupEntityCollection;
import com.microfocus.octane.plugins.rest.query.QueryPhrase;

public interface OctaneRestService {

	GroupEntityCollection getCoverage(String applicationModulePath);

	OctaneEntityCollection getEntitiesByCondition(String collectionName, QueryPhrase phrase);

	void reloadConfiguration();
}