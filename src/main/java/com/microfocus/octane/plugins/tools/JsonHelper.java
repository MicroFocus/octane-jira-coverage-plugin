package com.microfocus.octane.plugins.tools;

import org.codehaus.jackson.map.ObjectMapper;

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

import java.io.IOException;

public class JsonHelper {

    public static String serialize(Object obj) throws IOException {
        //plugin setting can save string with length upto 99000.
        //length of pretty format is greater upto 40% in comparison with regular format
        ObjectMapper mapper = new ObjectMapper();
        //String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        String json = mapper.writer().writeValueAsString(obj);
        return json;
    }

    public static <T> T deserialize(String value, Class<T> valueType) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        T result = mapper.readValue(value, valueType);
        return result;
    }


}
