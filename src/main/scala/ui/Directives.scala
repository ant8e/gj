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

import spray.routing._
import Directives._

import spray.http._
import HttpHeaders._
import akka.actor._
import scala.concurrent.duration._
import util.{ Success, Failure }
import spray.can.Http

// Enable scala features

import scala.language.postfixOps
import scala.language.implicitConversions

trait ServerSideEventsDirectives {

  case class Message(data: String, event: Option[String], id: Option[String])

  case class RegisterClosedHandler(handler: () ⇒ Unit)

  object CloseConnection

  object Message {
    def apply(data: String): Message = Message(data, None, None)

    def apply(data: String, event: String): Message = Message(data, Some(event), None)

    def apply(data: String, event: String, id: String): Message = Message(data, Some(event), Some(id))
  }

  def sse(body: (ActorRef, Option[String]) ⇒ Unit)(implicit refFactory: ActorRefFactory): Route = {

    val responseStart = HttpResponse(
      headers = `Cache-Control`(CacheDirectives.`no-cache`) :: Nil,
      entity = ":" + (" " * 2049) + "\n" // 2k padding for IE using Yaffle
      )

    // TODO These headers should be standard headers
    val preflightHeaders = List(
      RawHeader("Access-Control-Allow-Methods", "GET"),
      RawHeader("Access-Control-Allow-Headers", "Last-Event-ID, Cache-Control"),
      RawHeader("Access-Control-Max-Age", "86400"))

    def lastEventId = optionalHeaderValueByName("Last-Event-ID") | parameter("lastEventId" ?)

    def sseRoute(lei: Option[String]) = (ctx: RequestContext) ⇒ {

      val connectionHandler = refFactory.actorOf(
        Props {
          new Actor {

            var closedHandlers: List[() ⇒ Unit] = Nil

            ctx.responder ! ChunkedResponseStart(responseStart)

            // Keep-Alive
            context.setReceiveTimeout(15 seconds)

            def receive = {
              case Message(data, event, id) ⇒
                val idString = id.map(id ⇒ s"id: $id\n").getOrElse("")
                val eventString = event.map(ev ⇒ s"event: $ev\n").getOrElse("")
                val dataString = data.split("\n").map(d ⇒ s"data: $d\n").mkString
                ctx.responder ! MessageChunk(s"${idString}${eventString}${dataString}\n")
              case CloseConnection ⇒
                ctx.responder ! ChunkedMessageEnd
              case ReceiveTimeout ⇒
                ctx.responder ! MessageChunk(":\n") // Comment to keep connection alive
              case RegisterClosedHandler(handler) ⇒ closedHandlers ::= handler
              case ev: Http.ConnectionClosed ⇒
                closedHandlers.foreach(_())
                context.stop(self)
            }
          }
        })

      body(connectionHandler, lei)
    }

    get {
      respondWithMediaType(MediaType.custom("text", "event-stream")) {
        // TODO This should be a standard media type
        lastEventId {
          lei ⇒
            sseRoute(lei)
        }
      }
    } ~
      // Answer preflight requests. Needed for Yaffle
      method(HttpMethods.OPTIONS) {
        // TODO Change this with options, that it's included in Master
        respondWithHeaders(preflightHeaders: _*) {
          complete(StatusCodes.OK)
        }
      }

  }
}

object ServerSideEventsDirectives extends ServerSideEventsDirectives
