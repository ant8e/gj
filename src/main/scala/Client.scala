/*
 * Copyright © 2014 Antoine Comte
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.net.InetSocketAddress
import scala.concurrent.duration._
import akka.io.{ Udp, IO }
import akka.util.{ ByteString, Timeout }
import akka.actor._
import akka.io.Udp.SimpleSenderReady

object Client extends App {
  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("metric-client")

  // we bind the server to a port on localhost and hook
  // in a continuation that informs us when bound
  val endpoint = new InetSocketAddress("localhost", 12344)
  implicit val bindingTimeout = Timeout(1.second)

  import system.dispatcher

  // execution context for the future

  val io: ActorRef = IO(Udp)

  class MySender extends Actor {
    def receive: Actor.Receive = {
      case "Go" ⇒ io ! Udp.SimpleSender
      case SimpleSenderReady ⇒ context.system.scheduler.schedule(0.seconds, 100.milliseconds, sender, Udp.Send(ByteString("test.bucket:1|c"), endpoint))
    }
  }

  system.actorOf(Props[MySender]) ! "Go"

}

