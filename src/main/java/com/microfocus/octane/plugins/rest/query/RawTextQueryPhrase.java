package com.microfocus.octane.plugins.rest.query;

public class RawTextQueryPhrase implements QueryPhrase {

	private String rawText;

	public RawTextQueryPhrase(String rawText) {
		this.rawText = rawText;
	}

	public String getRawText() {
		return rawText;
	}
}
