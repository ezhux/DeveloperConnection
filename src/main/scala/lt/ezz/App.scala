package lt.ezz

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http

import scala.io.StdIn

object App {

  implicit val system = ActorSystem(Behaviors.empty, "actor-system")
  implicit val executionContext = system.executionContext

  def main(args: Array[String]): Unit = {

    val dc = new DeveloperConnection()
    val bindingFuture = Http().newServerAt("localhost", 8080).bind(dc.route)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine()
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }

}

