package com.microfocus.octane.plugins.rest.query;

public class NullQueryPhrase implements QueryPhrase {

	public static NullQueryPhrase get()
	{
		return new NullQueryPhrase();
	}
}
