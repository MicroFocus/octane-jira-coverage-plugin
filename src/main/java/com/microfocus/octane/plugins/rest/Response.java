/*
 *     Copyright 2018 EntIT Software LLC, a Micro Focus company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */


package com.microfocus.octane.plugins.rest;

import java.util.Map;

/**
 * Container of the response headers and the response body.
 */
public class Response {

    private Map<String, ? extends Iterable<String>> responseHeaders = null;
    private String responseData = null;
    private Exception failure = null;
    private int statusCode = 0;

    /**
     * @param responseHeaders
     * @param responseData
     * @param failure
     */
    public Response(
            Map<String, Iterable<String>> responseHeaders,
            String responseData,
            Exception failure,
            int statusCode) {
        super();
        this.responseHeaders = responseHeaders;
        this.responseData = responseData;
        this.failure = failure;
        this.statusCode = statusCode;
    }

    public Response() {
    }

    /**
     * @return the responseHeaders
     */
    public Map<String, ? extends Iterable<String>> getResponseHeaders() {
        return responseHeaders;
    }

    /**
     * @param responseHeaders the responseHeaders to set
     */
    public void setResponseHeaders(Map<String, ? extends Iterable<String>> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    /**
     * @return the responseData
     */
    public String getResponseData() {
        return responseData;
    }

    /**
     * @param responseData the responseData to set
     */
    public void setResponseData(String responseData) {
        this.responseData = responseData;
    }

    /**
     * @return the failure if the access to the requested url somehow failed, such as a 404 or 500
     * if no such failure occured this method returns null.
     */
    public Exception getFailure() {
        return failure;
    }

    /**
     * @param failure the failure to set
     */
    public void setFailure(Exception failure) {
        this.failure = failure;
    }

    /**
     * @return the statusCode
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * @param statusCode the statusCode to set
     */
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * @see Object#toString() return the contents of the byte[] data as a string.
     */
    @Override
    public String toString() {

        return this.responseData == null ? "responseData is null" : this.responseData;
    }

}