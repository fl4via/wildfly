/*
 * Copyright 2019 Red Hat, Inc.
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
package org.wildfly.clustering.ejb.infinispan;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Flavia Rainone
 */
public class ExpirationMap {

    private final long timeout;
    private final Map<Long, LinkNode> linkNodeMap;
    private LinkNode head;
    private LinkNode tail;

    private static class LinkNode {
        LinkNode next;
        LinkNode previous;
        long expiration;
        final long sessionId;

        public LinkNode(long sessionId) {
            this.sessionId = sessionId;
        }
    }

    public ExpirationMap(long timeout) {
        this.timeout = timeout;
        linkNodeMap = new HashMap<>();
    }

    public void resetExpiration(long sessionId) {
        final LinkNode linkNode;
        synchronized (this) {
            if (linkNodeMap.containsKey(sessionId)) {
                linkNode = linkNodeMap.get(sessionId);
                    if (linkNode.next != null) {
                        linkNode.next.previous = linkNode.previous;
                    }
                    if (linkNode.previous != null) {
                        linkNode.previous.next = linkNode.next;
                    } else if (head == linkNode && head != tail) {
                        head = head.next;
                    }
                    linkNode.previous = null;

            } else {
                linkNode = new LinkNode(sessionId);
                linkNodeMap.put(sessionId, linkNode);
            }
            // if there is a single node in the list, no need to change it
            if (tail != null && tail != linkNode) {
                linkNode.previous = tail;
                tail.next = linkNode;
                tail = linkNode;
            } else if (tail == null) {
                head = tail = linkNode;
            }
        }
        linkNode.expiration = System.nanoTime() + timeout;
    }

    public long getExpiredSessionId() {
        // if head is expired
        synchronized (this) {
            if (head != null && head.expiration <= System.nanoTime()) {
                final long expiredSessionId = head.sessionId;
                head = head.next;
                linkNodeMap.remove(expiredSessionId);
                head.previous = null;
                return expiredSessionId;
            }
            return -1;
        }
    }

}
