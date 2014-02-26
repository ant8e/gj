import org.scalatest.FunSpec
import org.scalatest.matchers.MustMatchers
import spray.http._
import spray.httpx.unmarshalling._

import spray.httpx.encoding.Gzip
import spray.testkit.ScalatestRouteTest
import ui.UIServerRoute

/**
 *
 */
class UiServerSpec extends FunSpec with ScalatestRouteTest with UIServerRoute with MustMatchers {
  def actorRefFactory = system // connect the DSL to the test ActorSystem


  val `text/html(UTF8)` = ContentType(MediaTypes.`text/html`, HttpCharsets.`UTF-8`)

  describe("the UI server") {
    it("should serve the index html file") {
      Get() ~>  routes ~> check {
        contentType must equal(`text/html(UTF8)`)
        Gzip.decode(response).as[String].right.get must include("<!doctype html>")
      }

    }
  }

}
