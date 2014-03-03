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

package ui

import gj.ActorSystemProvider
import spray.routing.{PathMatchers, HttpService, SimpleRoutingApp}
import akka.actor.{ActorRef, Actor, ActorRefFactory, Props}
import ServerSideEventsDirectives._

/**
 *
 */

trait UIServerRoute extends HttpService {
  lazy val staticRoutes = get {
    decompressRequest()
    compressResponse() {
      pathSingleSlash {
        getFromResource("web/index.html")
      } ~
        getFromResourceDirectory("web")
    }
  }
  lazy val apiRoutes = pathPrefix("api") {
    get {
      _.complete("Todo")
    }
  }

  lazy val valueRoute = path("bucket" / PathMatchers.RestPath) {
    b =>
      val bucket = b.toString()

      sse((channel, _) =>
        implicitly[ActorRefFactory].actorOf(Props(new ValueActor(channel,bucket)))
      )
  }

  lazy val routes = apiRoutes ~ valueRoute ~ staticRoutes

}

trait UiServer extends SimpleRoutingApp with UIServerRoute {
  self: ActorSystemProvider with UiServerConfiguration =>


  implicit val sprayActorSystem = this.actorSystem
  startServer("", UiServerPort)(routes)

}

trait UiServerConfiguration {
  def UiServerPort: Int
}

class ValueActor(channel: ActorRef, bucket:String ) extends Actor {

  import scala.concurrent.duration._
  import context._

  system.scheduler.schedule(1.second, 1.second, self, "tick")


  channel ! RegisterClosedHandler(() => context.stop(self))

  override def receive: Actor.Receive = {
    case "tick" => channel ! Message(s"hello from $bucket")
  }

}
