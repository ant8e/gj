package ui

import gj.ActorSystemProvider
import spray.routing.{HttpService, SimpleRoutingApp}

/**
 *
 */

trait UIServerRoute extends HttpService {
  lazy val staticRoutes = get {
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

  lazy val routes = apiRoutes ~ staticRoutes

}

trait UiServer extends SimpleRoutingApp with UIServerRoute {
  self: ActorSystemProvider with UiServerConfiguration =>


  implicit val sprayActorSystem = this.actorSystem
  startServer("", UiServerPort)(routes)

}

trait UiServerConfiguration {
  def UiServerPort: Int
}
