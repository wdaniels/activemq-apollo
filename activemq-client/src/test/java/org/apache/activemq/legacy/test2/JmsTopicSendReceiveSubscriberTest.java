/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.legacy.test2;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Topic;
import javax.jms.TopicSession;


/**
 * @version $Revision: 1.4 $
 */
public class JmsTopicSendReceiveSubscriberTest extends JmsTopicSendReceiveTest {
    protected MessageConsumer createConsumer() throws JMSException {
        if (durable) {
            return super.createConsumer();
        } else {
            TopicSession topicSession = (TopicSession)session;
            return topicSession.createSubscriber((Topic)consumerDestination, null, false);
        }
    }
}