/*******************************************************************************
 * Copyright 2017-2023 Open Text.
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors (“Open Text”) are as may be set forth
 * in the express warranty statements accompanying such products and services.
 * Nothing herein should be construed as constituting an additional warranty.
 * Open Text shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Except as specifically indicated otherwise, this document contains
 * confidential information and a valid license is required for possession,
 * use or copying. If this work is provided to the U.S. Government,
 * consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial Items are
 * licensed to the U.S. Government under vendor's standard commercial license.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *   http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/


package com.microfocus.octane.plugins.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

;

/**
 * Thrown if status is not 200 or 201
 * Created by berkovir on 20/11/2016.
 */
public class RestStatusException extends RuntimeException {
    private Response response;
    private String error_code;
    private String description;

    public RestStatusException(Response response) {
        super(response.getResponseData());
        this.response = response;

        try {
            if (response.getResponseData().startsWith("{")) {
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> statusData = objectMapper.readValue(response.getResponseData(), new TypeReference<Map<String, Object>>() {});
                error_code = (String) statusData.get("error_code");
                description = (String) statusData.get("description");
            }
        } catch (Exception e) {
            //do nothing
        }
    }

    public Response getResponse() {
        return response;
    }

    public String getErrorCode() {
        return error_code;
    }

    public String getDescription() {
        return description;
    }

    public int getStatus() {
        return this.response.getStatusCode();
    }
}


