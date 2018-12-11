package com.microfocus.octane.plugins.rest.query;

import java.util.Collection;

public class InQueryPhrase implements QueryPhrase {

	private String fieldName;
	private Collection<String> values;

	public InQueryPhrase(String fieldName, Collection<String> values)
	{
		this.fieldName = fieldName;
		this.values = values;
	}

	public String getFieldName() {
		return fieldName;
	}

	public Collection<String> getValues() {
		return values;
	}
}
