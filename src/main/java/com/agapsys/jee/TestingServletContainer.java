/*
 * Copyright 2017 Agapsys Tecnologia Ltda-ME.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.agapsys.jee;

import com.agapsys.http.HttpClient;
import com.agapsys.http.HttpRequest;
import com.agapsys.http.HttpResponse;
import com.agapsys.http.HttpResponse.StringResponse;
import java.io.IOException;
import javax.servlet.http.HttpServlet;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

/**
 *
 * @author Leandro Oliveira (leandro@agapsys.com)
 */
public class TestingServletContainer<TC extends TestingServletContainer<TC>> extends ServletContainer<TestingServletContainer<TC>> {

    public static TestingServletContainer<?> newInstance(Class<? extends HttpServlet>...servlets) {
        return new TestingServletContainer(servlets);
    }

    protected TestingServletContainer(Class<? extends HttpServlet>...servlets) {
        super(servlets);
    }

    @Override
    protected Connector[] getConnectors(Server server) {
        ServerConnector http = new ServerConnector(server);
        http.setPort(0);
        return new Connector[] { http };
    }

    public int getRunningPort() {
        if (!isRunning())
            throw new IllegalStateException("Container is not running");

        return ((ServerConnector)super.getConnectors()[0]).getLocalPort();
    }

    /**
     * Perform a request against this servlet container.
     *
     * @param client {@linkplain HttpClient} instance
     * @param request {@linkplain HttpRequest} instance
     * @return response
     */
    public StringResponse doRequest(HttpClient client, HttpRequest request) {
        if (!isRunning()) {
            throw new IllegalStateException("Server is not running");
        }

        // Change URI to use servlet container
        String oldUri = request.getUri();

        if (oldUri == null || oldUri.isEmpty()) {
            throw new IllegalArgumentException("Null/Empty uri");
        }

        if (oldUri.contains(":") || oldUri.contains(" ") || !oldUri.startsWith("/")) {
            throw new IllegalArgumentException("Invalid uri: " + oldUri);
        }

        request.setUri(String.format("http://127.0.0.1:%d%s", getRunningPort(), oldUri));

        HttpResponse.StringResponse resp;
        try {
            resp = HttpResponse.getStringResponse(client, request, "utf-8", -1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Restore servlet URI
        request.setUri(oldUri);

        return resp;
    }

    /**
     * Perform a request against this servlet container.
     *
     * @param request {@linkplain HttpRequest} instance
     * @return response
     */
    public StringResponse doRequest(HttpRequest request) {
        try {
            HttpClient client = new HttpClient();
            HttpResponse.StringResponse response = doRequest(client, request);
            client.close();
            return response;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
}

}
