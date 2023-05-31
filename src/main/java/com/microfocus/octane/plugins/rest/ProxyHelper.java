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

import com.microfocus.octane.plugins.configuration.ConfigurationManager;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class ProxyHelper {

    private static final Logger log = LoggerFactory.getLogger(ProxyHelper.class);

    public static ProxyConfiguration getProxyConfiguration(URL targetUrl) {
        log.info("get proxy configuration for " + targetUrl.getHost());
        ProxyConfiguration result = null;

        ProxyConfiguration proxyConfig = ConfigurationManager.getInstance().getProxySettings();
        if (proxyConfig != null && StringUtils.isNotEmpty(proxyConfig.getHost())) {
            if (StringUtils.isNotEmpty(proxyConfig.getNonProxyHost()) && isNonProxyHost(targetUrl.getHost(), proxyConfig.getNonProxyHost())) {
                log.info(targetUrl.getHost() + " is NonProxyHost");
            } else {
                result = proxyConfig;
                log.info("proxy settings : " + result.toString());
            }
        } else if (isProxyNeededInJVM(targetUrl)) {
            String protocol = targetUrl.getProtocol().toLowerCase();
            result = ProxyConfiguration.create()
                    .setHost(getProxyProperty(protocol + ".proxyHost", null))
                    .setPort(Integer.parseInt(getProxyProperty(protocol + ".proxyPort", null)))
                    .setUsername(System.getProperty(protocol + ".proxyUser", ""))
                    .setPassword(System.getProperty(protocol + ".proxyPassword", ""));

            log.info("proxy settings from JVM : " + result.toString());
        } else {
            log.info("no proxy settings");
        }
        return result;
    }

    private static String getProxyProperty(String propKey, String def) {
        if (def == null) {
            def = "";
        }

        return System.getProperty(propKey) != null ? System.getProperty(propKey).trim() : def;
    }

    private static boolean isProxyNeededInJVM(URL targetHostUrl) {
        boolean result = false;
        String proxyHost = getProxyProperty(targetHostUrl.getProtocol() + ".proxyHost", "");
        String nonProxyHostsStr = getProxyProperty(targetHostUrl.getProtocol() + ".nonProxyHosts", "");

        if (StringUtils.isNotEmpty(proxyHost) && !isNonProxyHost(targetHostUrl.getHost(), nonProxyHostsStr)) {
            result = true;
            log.info("proxy is required for host " + targetHostUrl.getHost());
        } else {
            log.info("proxy is not  required for host " + targetHostUrl.getHost());
        }
        log.info("nonProxyHosts= " + nonProxyHostsStr);

        return result;
    }

    private static boolean isNonProxyHost(String targetHost, String nonProxyHostsStr) {
        boolean noProxyHost = false;
        List<Pattern> noProxyHosts = new LinkedList<>();
        if (nonProxyHostsStr != null && !nonProxyHostsStr.isEmpty()) {
            String[] hosts = nonProxyHostsStr.split("[ \t\n,|]+");
            for (String host : hosts) {
                if (!host.isEmpty()) {
                    noProxyHosts.add(Pattern.compile(host.replace(".", "\\.").replace("*", ".*")));
                }
            }
        }
        for (Pattern pattern : noProxyHosts) {
            if (pattern.matcher(targetHost).find()) {
                noProxyHost = true;
                break;
            }
        }
        return noProxyHost;
    }

}
