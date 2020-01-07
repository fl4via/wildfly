/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.undertow;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import io.undertow.server.ConnectorStatistics;
import io.undertow.server.ExchangeInfo;

import static org.wildfly.extension.undertow.ListenerResourceDefinition.ACTIVE_REQUEST;
import static org.wildfly.extension.undertow.ListenerResourceDefinition.ACTIVE_REQUEST_BYTES_RECEIVED;
import static org.wildfly.extension.undertow.ListenerResourceDefinition.ACTIVE_REQUEST_CONTENT_LENGTH;
import static org.wildfly.extension.undertow.ListenerResourceDefinition.ACTIVE_REQUEST_CURRENT_QUERY;
import static org.wildfly.extension.undertow.ListenerResourceDefinition.ACTIVE_REQUEST_CURRENT_URI;
import static org.wildfly.extension.undertow.ListenerResourceDefinition.ACTIVE_REQUEST_HOST_NAME;
import static org.wildfly.extension.undertow.ListenerResourceDefinition.ACTIVE_REQUEST_HOST_PORT;
import static org.wildfly.extension.undertow.ListenerResourceDefinition.ACTIVE_REQUEST_METHOD;
import static org.wildfly.extension.undertow.ListenerResourceDefinition.ACTIVE_REQUEST_PROCESSING_TIME;
import static org.wildfly.extension.undertow.ListenerResourceDefinition.ACTIVE_REQUEST_PROTOCOL;

/**
 * Lists the active requests associated with a listener.
 *
 * @author Flavia Rainone
 */
public class ActiveRequestListHandler implements OperationStepHandler {
    public static final String OPERATION_NAME = "active-request-list";


    public static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(OPERATION_NAME, UndertowExtension.getResolver("listener"))
            .setReadOnly()
            .setRuntimeOnly()
            .setReplyType(ModelType.LIST)
            .setReplyParameters(ACTIVE_REQUEST.getValueTypes())
            .build();
    public static final ActiveRequestListHandler INSTANCE = new ActiveRequestListHandler();

    private ActiveRequestListHandler() {

    }

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final ListenerService service = ListenerResourceDefinition.getListenerService(context);
        final ExchangeInfo[] exchangeInfos;
        if (service != null) {
            ConnectorStatistics stats = service.getOpenListener().getConnectorStatistics();
            if (stats != null) {
                exchangeInfos = stats.getActiveRequestInfo();
            } else
                exchangeInfos = null;
        } else
            exchangeInfos = null;
        context.addStep(new OperationStepHandler() {
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                final ModelNode result = context.getResult();
                for (ExchangeInfo exchangeInfo : exchangeInfos) {
                    final ModelNode requestNode = new ModelNode();
                    requestNode.get(ACTIVE_REQUEST_BYTES_RECEIVED.getName()).set(exchangeInfo.getBytesReceived());
                    requestNode.get(ACTIVE_REQUEST_CONTENT_LENGTH.getName()).set(exchangeInfo.getContentLength());
                    requestNode.get(ACTIVE_REQUEST_CURRENT_URI.getName()).set(exchangeInfo.getCurrentUri());
                    requestNode.get(ACTIVE_REQUEST_CURRENT_QUERY.getName()).set(exchangeInfo.getCurrentQueryString());
                    requestNode.get(ACTIVE_REQUEST_METHOD.getName()).set(exchangeInfo.getMethod());
                    requestNode.get(ACTIVE_REQUEST_PROCESSING_TIME.getName()).set(exchangeInfo.getProcessingTime());
                    requestNode.get(ACTIVE_REQUEST_PROTOCOL.getName()).set(exchangeInfo.getProtocol());
                    requestNode.get(ACTIVE_REQUEST_HOST_NAME.getName()).set(exchangeInfo.getHostName());
                    requestNode.get(ACTIVE_REQUEST_HOST_PORT.getName()).set(exchangeInfo.getHostPort());
                    result.add(requestNode);
                }
            }
        }, OperationContext.Stage.RUNTIME);
    /*} else {
        context.getFailureDescription().set(ConnectorLogger.ROOT_LOGGER.noMetricsAvailable());
    }*/
    context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }

}
