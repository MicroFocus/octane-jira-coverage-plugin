package com.microfocus.octane.plugins.rest.query;

public class NegativeQueryPhrase implements QueryPhrase {

	private QueryPhrase queryPhrase ;

	public NegativeQueryPhrase(QueryPhrase phrase)
	{
		this.queryPhrase = phrase;
	}

	public QueryPhrase getQueryPhrase() {
		return queryPhrase;
	}
}
