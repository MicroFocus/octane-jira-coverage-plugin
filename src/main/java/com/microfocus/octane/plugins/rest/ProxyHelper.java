package com.microfocus.octane.plugins.rest;

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


        if (isProxyNeeded(targetUrl)) {
            String protocol = targetUrl.getProtocol().toLowerCase();

            result = ProxyConfiguration.create()
                    .setHost(getProxyProperty(protocol + ".proxyHost", null))
                    .setPort(Integer.parseInt(getProxyProperty(protocol + ".proxyPort", null)))
                    .setUsername(System.getProperty(protocol + ".proxyUser", ""))
                    .setPassword(System.getProperty(protocol + ".proxyPassword", ""));

            log.info("proxy settings : " + result.toString());
        } else {
            log.info("no proxy settings.");
        }
        return result;
    }

    private static String getProxyProperty(String propKey, String def) {
        if (def == null) {
            def = "";
        }

        return System.getProperty(propKey) != null ? System.getProperty(propKey).trim() : def;
    }

    private static boolean isProxyNeeded(URL targetHostUrl) {
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
