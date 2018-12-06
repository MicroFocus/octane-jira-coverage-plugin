/*
 *     Copyright 2017 EntIT Software LLC, a Micro Focus company, L.P.
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
 *
 */


package com.microfocus.octane.plugins.rest;

/**
 * Thrown if status is not 200 or 201
 * Created by berkovir on 20/11/2016.
 */
public class RestStatusException extends RuntimeException {
    private Response response;

    public RestStatusException(Response response ){
        super(response.getResponseData());
        this.response = response;
    }
    public Response getResponse() {
        return response;
    }
}
