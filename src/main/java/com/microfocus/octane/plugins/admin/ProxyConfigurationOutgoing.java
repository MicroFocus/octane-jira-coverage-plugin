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


package com.microfocus.octane.plugins.admin;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ProxyConfigurationOutgoing {

    @XmlElement(name = "host")
    private String host;

    @XmlElement(name = "port")
    private String port;

    @XmlElement(name = "username")
    private String username;

    @XmlElement(name = "password")
    private String password;


    public static ProxyConfigurationOutgoing create() {
        return new ProxyConfigurationOutgoing();
    }

    public String getHost() {
        return host;
    }

    public ProxyConfigurationOutgoing setHost(String host) {
        this.host = host;
        return this;
    }

    public String getPort() {
        return port;
    }

    public ProxyConfigurationOutgoing setPort(String port) {
        this.port = port;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public ProxyConfigurationOutgoing setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public ProxyConfigurationOutgoing setPassword(String password) {
        this.password = password;
        return this;
    }

    @Override
    public String toString() {
        return "ProxyConfiguration {host=" + host + ", port=" + port + ", username=" + username + " }";
    }
}


