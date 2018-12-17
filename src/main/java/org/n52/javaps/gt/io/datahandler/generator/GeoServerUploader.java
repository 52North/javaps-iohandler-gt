/*
 * Copyright 2018 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.n52.javaps.gt.io.datahandler.generator;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.n52.javaps.annotation.ConfigurableClass;
import org.n52.javaps.annotation.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Properties(
        propertyFileName = "geoserver.properties",
        defaultPropertyFileName = "geoserver.default.properties")
public class GeoServerUploader implements ConfigurableClass {

    private static final String CONTENT_TYPE = "Content-type";

    private static Logger LOGGER = LoggerFactory.getLogger(GeoServerUploader.class);

    private CloseableHttpClient httpClient;

    private String geoServerURL;

    private String workspaceName;

    public GeoServerUploader() throws MalformedURLException {

        geoServerURL = getProperties().get("geoserverurl").asText();
        workspaceName = getProperties().get("workspacename").asText();

        URL url;
        try {
            url = new URL(geoServerURL);
        } catch (MalformedURLException e) {
            String message = "Could not create URL out of String: " + geoServerURL;
            LOGGER.error(message);
            throw new MalformedURLException(message);
        }

        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

        String username = getProperties().get("username").asText();
        String password = getProperties().get("password").asText();

        credentialsProvider.setCredentials(new AuthScope(url.getHost(), url.getPort()), new UsernamePasswordCredentials(
                username, password));
        HttpClientContext localContext = HttpClientContext.create();
        localContext.setCredentialsProvider(credentialsProvider);

        httpClient = HttpClients.custom().setDefaultCredentialsProvider(credentialsProvider).build();
    }

    public String uploadGeotiff(File file,
            String storeName) throws HttpException, IOException {
        String target = geoServerURL + "/rest/workspaces/N52/coveragestores/" + storeName
                + "/external.geotiff?configure=first&coverageName=" + storeName;
        String request;
        if (file.getAbsolutePath().startsWith("/")) {
            request = "file:" + file.getAbsolutePath();
        } else {
            request = "file:/" + file.getAbsolutePath();
        }
        String result = sendRasterRequest(target, request);
        return result;
    }

    public String uploadShp(File file,
            String storeName) throws HttpException, IOException {
        String target = geoServerURL + "/rest/workspaces/N52/datastores/" + storeName + "/file.shp";
        InputStream request = new BufferedInputStream(new FileInputStream(file));
        String result = sendShpRequest(target, request);
        return result;

    }

    private String sendPutRequest(String target,
            InputStream request,
            String contentType) throws ClientProtocolException, IOException {

        HttpPut put = new HttpPut(target);
        put.addHeader(CONTENT_TYPE, contentType);

        put.setEntity(new BufferedHttpEntity(new InputStreamEntity(request)));

        CloseableHttpResponse response = httpClient.execute(put);

        InputStream responseObject = response.getEntity().getContent();

        StringWriter writer = new StringWriter();

        String encoding = StandardCharsets.UTF_8.name();

        IOUtils.copy(responseObject, writer, encoding);

        return writer.toString();
    }

    private String sendPutRequest(String target,
            String request,
            String contentType) throws ClientProtocolException, IOException {

        InputStream inputStream = new ByteArrayInputStream(request.getBytes());

        return sendPutRequest(target, inputStream, contentType);
    }

    public String createWorkspace() throws HttpException, IOException {
        String target = geoServerURL + "/rest/workspaces";
        String request = "<workspace><name>" + workspaceName + "</name></workspace>";

        HttpPost post = new HttpPost(target);
        post.addHeader(CONTENT_TYPE, "application/xml");

        post.setEntity(new StringEntity(request));

        CloseableHttpResponse response = httpClient.execute(post);

        InputStream responseObject = response.getEntity().getContent();

        StringWriter writer = new StringWriter();
        String encoding = StandardCharsets.UTF_8.name();
        IOUtils.copy(responseObject, writer, encoding);

        return writer.toString();
    }

    private String sendRasterRequest(String target,
            String request) throws HttpException, IOException {

        return sendPutRequest(target, request, "text/plain");
    }

    private String sendShpRequest(String target,
            InputStream request) throws HttpException, IOException {

        return sendPutRequest(target, request, "application/zip");
    }

    public String getGeoServerURL() {
        return geoServerURL;
    }

    public void setGeoServerURL(String geoServerURL) {
        this.geoServerURL = geoServerURL;
    }
}
