/*
 * Copyright (C) 2016-2020 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *       • Apache License, version 2.0
 *       • Apache Software License, version 1.0
 *       • GNU Lesser General Public License, version 3
 *       • Mozilla Public License, versions 1.0, 1.1 and 2.0
 *       • Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public
 * License version 2 and the aforementioned licenses.
 *
 * As an exception to the terms of the GPL, you may copy, modify,
 * propagate, and distribute a work formed by combining 52°North WPS
 * GeoTools Modules with the Eclipse Libraries, or a work derivative of
 * such a combination, even if such copying, modification, propagation, or
 * distribution would otherwise violate the terms of the GPL. Nothing in
 * this exception exempts you from complying with the GPL in all respects
 * for all of the code used other than the Eclipse Libraries. You may
 * include this exception and its grant of permissions when you distribute
 * 52°North WPS GeoTools Modules. Inclusion of this notice with such a
 * distribution constitutes a grant of such permissions. If you do not wish
 * to grant these permissions, remove this paragraph from your
 * distribution. "52°North WPS GeoTools Modules" means the 52°North WPS
 * modules using GeoTools functionality - software licensed under version 2
 * or any later version of the GPL, or a work based on such software and
 * licensed under the GPL. "Eclipse Libraries" means Eclipse Modeling
 * Framework Project and XML Schema Definition software distributed by the
 * Eclipse Foundation and licensed under the Eclipse Public License Version
 * 1.0 ("EPL"), or a work based on such software and licensed under the EPL.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
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
        propertyFileName = "geoserver.json",
        defaultPropertyFileName = "geoserver.default.json")
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

        InputStream inputStream = new ByteArrayInputStream(request.getBytes(StandardCharsets.UTF_8));

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
