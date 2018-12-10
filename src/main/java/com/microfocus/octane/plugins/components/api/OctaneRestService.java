package com.microfocus.octane.plugins.components.api;

import com.microfocus.octane.plugins.rest.entities.OctaneEntity;
import com.microfocus.octane.plugins.rest.entities.groups.GroupEntityCollection;

public interface OctaneRestService {

	GroupEntityCollection getCoverage(String applicationModulePath);

	OctaneEntity getEntityById(String collectionName,String entityId);

	void reloadConfiguration();
}