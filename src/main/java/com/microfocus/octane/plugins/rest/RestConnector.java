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

import com.google.common.base.Charsets;
import com.microfocus.octane.plugins.configuration.PluginConstants;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.Map.Entry;


public class RestConnector {

    public final static String HEADER_ACCEPT = "Accept";
    public final static String HEADER_APPLICATION_JSON = "application/json";
    public final static String HEADER_APPLICATION_XML = "application/xml";
    public final static String HEADER_CONTENT_TYPE = "Content-Type";

    protected Map<String, String> cookies = new HashMap<>();

    private String baseUrl;
    private String user;
    private String password;
    private ProxyConfiguration proxyConfiguration;
    private SSLContext sslContext;
    private static final Logger log = LoggerFactory.getLogger(RestConnector.class);

    public boolean login() {
        boolean ret = false;
        clearAll();
        ObjectMapper mapper = new ObjectMapper();
        String jsonString = null;
        HashMap<String, String> authData = new HashMap<>();
        authData.put("user", user);
        authData.put("password", password);
        try {
            jsonString = mapper.writeValueAsString(authData);
        } catch (IOException e) {
            throw new RuntimeException("Fail in generating json for login data : " + e.getMessage());
        }

        //Get LWSSO COOKIE
        Map<String, String> headers = new HashMap<>();
        headers.put(HEADER_CONTENT_TYPE, HEADER_APPLICATION_JSON);
        Response authResponse = doHttp("POST", PluginConstants.URL_AUTHENTICATION, null, jsonString, headers, true);
        if (authResponse.getStatusCode() == HttpStatus.SC_OK) {
            ret = true;
        }

        return ret;
    }

    /**
     * @return the cookies
     */
    public Map<String, String> getCookies() {
        return cookies;
    }

    /**
     * @param cookies the cookies to set
     */
    public void setCookies(Map<String, String> cookies) {
        this.cookies = cookies;
    }

    public Response httpPut(String url, String data, Map<String, String> headers) {

        return doHttp("PUT", url, null, data, headers);
    }

    public Response httpPost(String url, String data, Map<String, String> headers) {

        return doHttp("POST", url, null, data, headers);
    }

    public Response httpDelete(String url, Map<String, String> headers) {

        return doHttp("DELETE", url, null, null, headers);
    }


    public Response httpGet(String url, List<String> queryParams) {

        return httpGet(url, queryParams, null);
    }

    public Response httpGet(String url, Collection<String> queryParams, Map<String, String> headers) {

        return doHttp("GET", url, queryParams, null, headers);
    }

    /**
     * @param type        of the http operation: get post put delete
     * @param url         to work on
     * @param queryParams
     * @param data        to write, if a writable operation
     * @param headers     to use in the request
     * @return http response
     */
    private Response doHttp(
            String type,
            String url,
            Collection<String> queryParams,
            String data,
            Map<String, String> headers) {
        return doHttp(type, url, queryParams, data, headers, false);
    }

    /**
     * @param type        of the http operation: get post put delete
     * @param url         to work on
     * @param queryParams
     * @param data        to write, if a writable operation
     * @param headers     to use in the request
     * @param relogin     if equal to false and received 401 and supportRelogin is exist - trial to relogin will be done
     * @return http response
     */
    private Response doHttp(
            String type,
            String url,
            Collection<String> queryParams,
            String data,
            Map<String, String> headers,
            boolean relogin) {


        //add query params
        if ((queryParams != null) && !queryParams.isEmpty()) {

            if (url.contains("?")) {
                url += "&";
            } else {
                url += "?";
            }
            url += StringUtils.join(queryParams, "&");
        }

        long start = System.currentTimeMillis();
        String fullUrl = baseUrl + url;
        try {

            HttpURLConnection con;
            if (proxyConfiguration == null) {
                con = (HttpURLConnection) new URL(fullUrl).openConnection();
            } else {
                try {
                    Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyConfiguration.getHost(), proxyConfiguration.getPort()));
                    con = (HttpURLConnection) new URL(fullUrl).openConnection(proxy);
                    if (StringUtils.isNotEmpty(proxyConfiguration.getUsername()) && StringUtils.isNotEmpty(proxyConfiguration.getPassword())) {
                        Authenticator authenticator = new Authenticator() {
                            public PasswordAuthentication getPasswordAuthentication() {
                                return (new PasswordAuthentication(proxyConfiguration.getUsername(), proxyConfiguration.getPassword().toCharArray()));
                            }
                        };
                        Authenticator.setDefault(authenticator);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Failed to define connection with proxy parameters");
                }
            }

            if (con instanceof HttpsURLConnection) {
                setSSLSocketFactory((HttpsURLConnection) con);
            }
            con.setRequestMethod(type);
            String cookieString = getCookieString();

            prepareHttpRequest(con, headers, data, cookieString);

            con.connect();
            Response ret = retrieveHtmlResponse(con);
            long end = System.currentTimeMillis();
            String msg = String.format("%s %s:%s , total time %s ms", ret.getStatusCode(), type, fullUrl, end - start);
            log.info(msg);

            updateCookies(ret);

            if (ret.getStatusCode() != HttpStatus.SC_OK && ret.getStatusCode() != HttpStatus.SC_CREATED && ret.getStatusCode() != HttpStatus.SC_ACCEPTED) {
                throw new RestStatusException(ret);
            }

            return ret;
        } catch (RestStatusException e) {
            if ((e.getResponse().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) ||
                    (e.getResponse().getStatusCode() == 0 && e.getResponse().getResponseData().equals("Error writing to server"))) {
                if (!relogin) {
                    boolean reloginResult = false;
                    try {
                        reloginResult = login();
                        String msg = String.format("Received status %s. Relogin succeeded.", e.getResponse().getStatusCode());
                        log.warn(msg);
                    } catch (Exception ex) {
                        String msg = String.format("Received status %s. Relogin failed %s", e.getResponse().getStatusCode(), ex.getMessage());
                        log.warn(msg);
                    }

                    if (reloginResult) {
                        return doHttp(type, url, queryParams, data, headers, true);
                    }
                }
            }
            throw e;//rethrow
        } catch (Exception e) {
            long end = System.currentTimeMillis();
            String msg = String.format("%s %s:%s , total time %s ms, %s", "ERR", type, fullUrl, end - start, e.getMessage());
            log.error(msg);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * @param con          to set the headers and bytes in
     * @param headers      to use in the request, such as content-TYPE
     * @param data         the actual data to post in the connection.
     * @param cookieString the cookies data from clientside, such as lwsso, qcsession, jsession etc..
     */
    private void prepareHttpRequest(
            HttpURLConnection con,
            Map<String, String> headers,
            String data,
            String cookieString) throws IOException {

        String contentType = null;

        //attach cookie information if such exists
        if ((cookieString != null) && !cookieString.isEmpty() && !hasCRLF(cookieString)) {
            con.setRequestProperty("Cookie", cookieString);
        }

        //send data from headers
        if (headers != null) {
            Iterator<Entry<String, String>> headersIterator = headers.entrySet().iterator();
            while (headersIterator.hasNext()) {
                Entry<String, String> header = headersIterator.next();
                if (header.getKey().equals("Content-Type")) {
                    //skip the content-TYPE header - should only be sent if you actually have any content to send. see below.
                    contentType = header.getValue();
                    continue;
                }
                con.setRequestProperty(header.getKey(), header.getValue());
            }
        }

        //if there's data to attach to the request, it's handled here. note that if data exists, we take into account previously removed content-TYPE.
        if ((data != null) && (!data.isEmpty())) {

            con.setDoOutput(true);

            //warning: if you add content-TYPE header then you MUST send information.. or receive error. so only do so if you're writing information...
            if (contentType != null) {
                con.setRequestProperty("Content-Type", contentType);
            }

            try (OutputStream out = con.getOutputStream()) {
                out.write(data.getBytes(Charsets.UTF_8));
            }
        }
    }

    /**
     * @param con that already connected to it's url with an http request, and that should contain a
     *            response for us to retrieve
     * @return a response from the server to the previously submitted http request
     * @throws Exception
     */
    private Response retrieveHtmlResponse(HttpURLConnection con) throws IOException {

        Response ret = new Response();

        //select the source of the input bytes, first try "regular" input
        try(InputStream inputStream = (con.getResponseCode() < HttpURLConnection.HTTP_BAD_REQUEST) ? con.getInputStream() : con.getErrorStream()) {

            //this actually takes the data from the previously decided stream (error or input) and stores it in a byte[] inside the response
            if (inputStream != null) {
                ByteArrayOutputStream container = new ByteArrayOutputStream();

                byte[] buffer = new byte[1024];
                int read;

                while ((read = inputStream.read(buffer, 0, 1024)) > 0) {
                    container.write(buffer, 0, read);
                }

                ret.setResponseData(container.toString(Charsets.UTF_8));
            }

        } catch (IOException e) {
            /*if the connection to the server somehow failed, for example 404 or 500, con.getInputStream() will throw an exception, which we'll keep.
            we'll also store the body of the exception page, in the response data. */
            ret.setFailure(e);
            ret.setResponseData(e.getMessage());//set default error message
        }

        try {
            ret.setStatusCode(con.getResponseCode());
        } catch (Exception e) {
            ret.setStatusCode(0);
        }

        ret.setResponseHeaders(con.getHeaderFields());

        return ret;
    }

    private void updateCookies(Response response) {
        updateCookies(response.getResponseHeaders().get("Set-Cookie"));
        updateCookies(response.getResponseHeaders().get("set-cookie"));
    }

    private void updateCookies(Iterable<String> newCookies) {
        if (newCookies != null) {

            for (String cookie : newCookies) {
                int equalIndex = cookie.indexOf('=');
                int semicolonIndex = cookie.indexOf(';');

                String cookieKey = cookie.substring(0, equalIndex);
                String cookieValue = cookie.substring(equalIndex + 1);
                if (semicolonIndex != -1) {
                    cookieValue = cookie.substring(equalIndex + 1, semicolonIndex);
                }
                cookies.put(cookieKey, cookieValue);
            }
        }
    }


    private String getCookieString() {

        StringBuilder sb = new StringBuilder();

        if (cookies != null && !cookies.isEmpty()) {

            Set<Entry<String, String>> cookieEntries = cookies.entrySet();
            for (Entry<String, String> entry : cookieEntries) {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append(";");
            }
        }

        String ret = sb.toString();

        return ret;
    }

    public void clearAll() {
        cookies = new HashMap<>();
    }

    /**
     * This is the url to the qc application. It will be something like http://myhost:8080/qcbin .
     * Make sure that there is no slash at the end
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String url) {

        this.baseUrl = url;
        if (StringUtils.isNotEmpty(baseUrl) && baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        try {
            proxyConfiguration = ProxyHelper.getProxyConfiguration(new URL(url));
        } catch (MalformedURLException e) {
            log.error("Failed to check getProxyConfiguration : " + e.getMessage());
        }
    }

    public String getUser() {
        return user;
    }

    public void setCredentials(String user, String password) {
        this.user = user;
        this.password = password;
    }

    private void setSSLSocketFactory(HttpsURLConnection httpsURLConnection) {
        if (sslContext == null) {
            try {
                sslContext = SSLContext.getInstance("TLSv1.2");
                sslContext.init(null, getTrustManagers(), new java.security.SecureRandom());

            } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException | RuntimeException e) {
                log.warn("Failed to create SSLContext " + e.getMessage());
            }
        }

        if (sslContext != null) {
            httpsURLConnection.setSSLSocketFactory(sslContext.getSocketFactory());
        }
    }

    private TrustManager[] getTrustManagers() throws NoSuchAlgorithmException, KeyStoreException {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init((KeyStore) null);
        TrustManager[] tmArr = tmf.getTrustManagers();
        if (tmArr.length == 1 && tmArr[0] instanceof X509TrustManager) {
            X509TrustManager defaultTm = (X509TrustManager) tmArr[0];
            TrustManager myTM = new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return defaultTm.getAcceptedIssuers();
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
                    defaultTm.checkClientTrusted(certs, authType);
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
                    try {
                        defaultTm.checkServerTrusted(certs, authType);
                    } catch (CertificateException e) {
                        for (X509Certificate cer : certs) {
                            if (cer.getIssuerDN().getName() != null && cer.getIssuerDN().getName().toLowerCase().contains("microfocus")) {
                                return;
                            }
                        }
                        throw e;
                    }
                }
            };

            return new TrustManager[]{myTM};
        } else {
            log.warn("Using only default trust managers. Received " + tmArr.length + " trust managers."
                    + ((tmArr.length > 0) ? "First one is :" + tmArr[0].getClass().getCanonicalName() : ""));
            return tmArr;
        }
    }

    private static boolean hasCRLF(String text) {
        return text.indexOf('\r') != -1 || text.indexOf('\n') != -1;
    }
}
