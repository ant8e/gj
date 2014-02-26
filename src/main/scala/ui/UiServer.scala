package ui

import gj.ActorSystemProvider
import spray.routing.SimpleRoutingApp

/**
 *
 */
trait UiServer extends SimpleRoutingApp {
  self: ActorSystemProvider with UiServerConfiguration =>

  lazy val staticRoutes = get {
    compressResponse() {
      path("") {
        getFromResource("web/index.html")
      } ~
        getFromResourceDirectory("web")
    }
  }
  lazy val apiRoutes = path("api") {
    get {
      _.complete("Todo")
    }
  }


  implicit val sprayActorSystem = this.actorSystem
  startServer("", UiServerPort)(apiRoutes ~ staticRoutes)

}

trait UiServerConfiguration {
  def UiServerPort :Int
}
