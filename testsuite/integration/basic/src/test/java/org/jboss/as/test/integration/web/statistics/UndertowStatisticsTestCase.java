/*
 * Copyright 2020 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.test.integration.web.statistics;

import java.net.URL;

import static java.util.concurrent.TimeUnit.SECONDS;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Flavia Rainone
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({UndertowStatisticsTestCase.StatisticsSetup.class})
public abstract class UndertowStatisticsTestCase {

    protected abstract void assertRequestProcessingTime(ModelNode requestProcessingTime);

    private static final ModelNode undertowSubsystemAddress = Operations.createAddress("subsystem", "undertow");
    private static final ModelNode httpListenerAddress = Operations.createAddress("subsystem", "undertow", "server", "default-server", "http-listener", "default");
    private static final ModelNode readRequestCountOperation = Operations.createReadAttributeOperation(httpListenerAddress, "request-count");
    private static final ModelNode readBytesSentOperation = Operations.createReadAttributeOperation(httpListenerAddress, "bytes-sent");
    private static final ModelNode readBytesReceivedOperation = Operations.createReadAttributeOperation(httpListenerAddress, "bytes-received");
    private static final ModelNode readErrorCountOperation = Operations.createReadAttributeOperation(httpListenerAddress, "error-count");
    private static final ModelNode readProcessingTimeOperation = Operations.createReadAttributeOperation(httpListenerAddress, "processing-time");
    private static final ModelNode readMaxProcessingTimeOperation = Operations.createReadAttributeOperation(httpListenerAddress, "max-processing-time");

    static class StatisticsSetup implements ServerSetupTask {
        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            final ModelControllerClient client = managementClient.getControllerClient();
            final ModelNode result = client.execute(Operations.createWriteAttributeOperation(undertowSubsystemAddress,
                    "statistics-enabled", "true"));
            assertTrue("Failed to enable statistics", Operations.isSuccessfulOutcome(result));
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            final ModelControllerClient client = managementClient.getControllerClient();
            final ModelNode result = client.execute(Operations.createWriteAttributeOperation(undertowSubsystemAddress,
                    "statistics-enabled", "false"));
            assertTrue("Failed to disable statistics", Operations.isSuccessfulOutcome(result));
        }
    }

    @ArquillianResource
    private ManagementClient client;
    @ArquillianResource
    private URL url;

    @Deployment
    public static WebArchive deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "anywar-example.war");
        war.addClasses(HttpRequest.class, EmptyServlet.class);
        return war;
    }

    private String performCall(String urlPattern) throws Exception {
        return HttpRequest.get(url.toExternalForm() + urlPattern, 10, SECONDS);
    }

    @Test
    public void oneRequestTest() throws Exception {
        final String result = performCall("empty");
        assertEquals("empty", result);

        final ModelControllerClient controllerClient = client.getControllerClient();

        final ModelNode requestCount = controllerClient.execute(readRequestCountOperation);
        assertTrue("Failed to read request-count", Operations.isSuccessfulOutcome(requestCount));
        assertTrue(Operations.readResult(requestCount).asLong() > 0);

        final ModelNode bytesSent = controllerClient.execute(readBytesSentOperation);
        assertTrue("Failed to read bytes-sent", Operations.isSuccessfulOutcome(bytesSent));
        assertTrue(Operations.readResult(bytesSent).asLong() > 0);

        final ModelNode bytesReceived = controllerClient.execute(readBytesReceivedOperation);
        assertTrue("Failed to read bytes-received", Operations.isSuccessfulOutcome(bytesReceived));
        assertTrue(Operations.readResult(bytesReceived).asLong() > 0);

        final ModelNode errorCount = controllerClient.execute(readErrorCountOperation);
        assertTrue("Failed to read error-count", Operations.isSuccessfulOutcome(errorCount));
        assertEquals(0, Operations.readResult(errorCount).asLong());

        final ModelNode processingTime = controllerClient.execute(readProcessingTimeOperation);
        assertTrue("Failed to read processing-time", Operations.isSuccessfulOutcome(processingTime));
        assertTrue(Operations.readResult(processingTime).asLong() > 0);

        final ModelNode maxProcessingTime = controllerClient.execute(readMaxProcessingTimeOperation);
        assertTrue("Failed to read processing-time", Operations.isSuccessfulOutcome(maxProcessingTime));
        assertTrue(Operations.readResult(maxProcessingTime).asLong() > 0);
    }

    @Test
    public void test() throws Exception {
        final String result = performCall("empty");
        assertEquals("empty", result);

        final ModelControllerClient controllerClient = client.getControllerClient();

        final ModelNode requestCount = controllerClient.execute(readRequestCountOperation);
        assertTrue("Failed to read request-count", Operations.isSuccessfulOutcome(requestCount));
        assertTrue(Operations.readResult(requestCount).asLong() > 0);

        final ModelNode bytesSent = controllerClient.execute(readBytesSentOperation);
        assertTrue("Failed to read bytes-sent", Operations.isSuccessfulOutcome(bytesSent));
        assertTrue(Operations.readResult(bytesSent).asLong() > 0);

        final ModelNode bytesReceived = controllerClient.execute(readBytesReceivedOperation);
        assertTrue("Failed to read bytes-received", Operations.isSuccessfulOutcome(bytesReceived));
        assertTrue(Operations.readResult(bytesReceived).asLong() > 0);

        final ModelNode errorCount = controllerClient.execute(readErrorCountOperation);
        assertTrue("Failed to read error-count", Operations.isSuccessfulOutcome(errorCount));
        assertEquals(0, Operations.readResult(errorCount).asLong());

        final ModelNode processingTime = controllerClient.execute(readProcessingTimeOperation);
        assertTrue("Failed to read processing-time", Operations.isSuccessfulOutcome(processingTime));
        assertTrue(Operations.readResult(processingTime).asLong() > 0);

        final ModelNode maxProcessingTime = controllerClient.execute(readMaxProcessingTimeOperation);
        assertTrue("Failed to read processing-time", Operations.isSuccessfulOutcome(maxProcessingTime));
        assertTrue(Operations.readResult(maxProcessingTime).asLong() > 0);
    }
}
