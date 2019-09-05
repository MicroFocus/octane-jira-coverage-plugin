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

public class ProxyConfiguration {

    private String host;

    private Integer port;

    private String username;

    private String password;

    private String nonProxyHost;

    public static ProxyConfiguration create() {
        return new ProxyConfiguration();
    }

    public String getHost() {
        return host;
    }

    public ProxyConfiguration setHost(String host) {
        this.host = host;
        return this;
    }

    public Integer getPort() {
        return port;
    }

    public ProxyConfiguration setPort(Integer port) {
        this.port = port;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public ProxyConfiguration setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public ProxyConfiguration setPassword(String password) {
        this.password = password;
        return this;
    }

    @Override
    public String toString() {
        return "ProxyConfiguration {host=" + host + ", port=" + port + ", username=" + username + " }";
    }

    public String getNonProxyHost() {
        return nonProxyHost;
    }

    public ProxyConfiguration setNonProxyHost(String nonProxyHost) {
        this.nonProxyHost = nonProxyHost;
        return this;
    }
}


