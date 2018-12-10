package com.microfocus.octane.plugins.rest.query;

/**
 *  Used to execute filter by cross entities, for example : get defects by "owner" name
 *
 */
public class CrossQueryPhrase implements QueryPhrase {

	private String fieldName;

	private QueryPhrase queryPhrase;

	public CrossQueryPhrase(String fieldName, QueryPhrase queryPhrase) {
		this.fieldName = fieldName;
		this.queryPhrase = queryPhrase;
	}

	public String getFieldName() {
		return fieldName;
	}

	public QueryPhrase getQueryPhrase() {
		return queryPhrase;
	}
}
