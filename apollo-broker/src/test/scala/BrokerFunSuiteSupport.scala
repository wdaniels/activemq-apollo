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
package org.apache.activemq.apollo.broker

import org.apache.activemq.apollo.util.{ServiceControl, Logging, FunSuiteSupport}
import java.net.InetSocketAddress
import org.apache.activemq.apollo.util._
import FileSupport._
import org.fusesource.hawtdispatch._
import org.apache.activemq.apollo.dto.{AggregateDestMetricsDTO, QueueStatusDTO, TopicStatusDTO}

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class BrokerFunSuiteSupport extends FunSuiteSupport with Logging { // with ShouldMatchers with BeforeAndAfterEach with Logging {
  var broker: Broker = null
  var port = 0

  def broker_config_uri = "xml:classpath:apollo.xml"

  def createBroker: Broker = {
    info("Loading broker configuration from the classpath with URI: " + broker_config_uri)
    var broker = BrokerFactory.createBroker(broker_config_uri)
    broker.setTmp(basedir / "target" / "tmp")
    broker.getTmp().mkdirs()
    broker
  }

  override protected def beforeAll() = {
    super.beforeAll()
    try {
      broker = createBroker
      ServiceControl.start(broker)
      port = broker.get_socket_address.asInstanceOf[InetSocketAddress].getPort
    } catch {
      case e: Throwable => e.printStackTrace
    }
  }

  override protected def afterAll() = {
    ServiceControl.stop(broker)
    super.afterAll()
  }

  def connector_port(connector: String): Option[Int] = Option(connector).map {
    id => broker.connectors.get(id).map(_.socket_address.asInstanceOf[InetSocketAddress].getPort).getOrElse(port)
  }

  def queue_exists(name: String): Boolean = {
    val host = broker.default_virtual_host
    host.dispatch_queue.future {
      val router = host.router.asInstanceOf[LocalRouter]
      router.local_queue_domain.destination_by_id.get(name).isDefined
    }.await()
  }

  def delete_queue(name: String) = {
    val host = broker.default_virtual_host
    host.dispatch_queue.future {
      val router = host.router.asInstanceOf[LocalRouter]
      for( node<- router.local_queue_domain.destination_by_id.get(name) ) {
        router._destroy_queue(node)
      }
    }.await()
  }

  def topic_exists(name: String): Boolean = {
    val host = broker.default_virtual_host
    host.dispatch_queue.future {
      val router = host.router.asInstanceOf[LocalRouter]
      router.local_topic_domain.destination_by_id.get(name).isDefined
    }.await()
  }

  def topic_status(name: String): TopicStatusDTO = {
    val host = broker.default_virtual_host
    sync(host) {
      val router = host.router.asInstanceOf[LocalRouter]
      router.local_topic_domain.destination_by_id.get(name).get.status
    }
  }

  def get_queue_metrics: AggregateDestMetricsDTO = {
    val host = broker.default_virtual_host
    sync(host) {
      host.get_queue_metrics
    }
  }

  def get_topic_metrics: AggregateDestMetricsDTO = {
    val host = broker.default_virtual_host
    sync(host) {
      host.get_topic_metrics
    }
  }

  def get_dsub_metrics: AggregateDestMetricsDTO = {
    val host = broker.default_virtual_host
    sync(host) {
      host.get_dsub_metrics
    }
  }

  def queue_status(name: String): QueueStatusDTO = {
    val host = broker.default_virtual_host
    sync(host) {
      val router = host.router.asInstanceOf[LocalRouter]
      val queue = router.local_queue_domain.destination_by_id.get(name).get
      sync(queue) {
        queue.status(false)
      }
    }
  }

  def dsub_status(name: String): QueueStatusDTO = {
    val host = broker.default_virtual_host
    sync(host) {
      val router = host.router.asInstanceOf[LocalRouter]
      router.local_dsub_domain.destination_by_id.get(name).get.status(false)
    }
  }

  def webadmin_uri(scheme:String = "http") = {
    Option(broker.web_server).flatMap(_.uris().find(_.getScheme == scheme)).get
  }

}
