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

package com.microfocus.octane.plugins.components.api;

public interface Constants {

    String URL_AUTHENTICATION = "/authentication/sign_in";

    String PUBLIC_API = "/api";
    String PUBLIC_API_SHAREDSPACE_FORMAT = PUBLIC_API + "/shared_spaces/%s";
    String PUBLIC_API_SHAREDSPACE_LEVEL_ENTITIES = PUBLIC_API + "/shared_spaces/%s" + "/%s";

    String PUBLIC_API_WORKSPACE_FORMAT = PUBLIC_API_SHAREDSPACE_FORMAT + "/workspaces/%s";
    String PUBLIC_API_WORKSPACE_LEVEL_ENTITIES = PUBLIC_API_WORKSPACE_FORMAT + "/%s";


}
