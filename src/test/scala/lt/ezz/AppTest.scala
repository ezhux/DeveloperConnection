package lt.ezz

import java.util.concurrent.TimeUnit

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import spray.json.DefaultJsonProtocol._
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.duration.FiniteDuration

class AppTest extends AnyWordSpec with Matchers with ScalatestRouteTest {

  implicit val timeout = RouteTestTimeout(FiniteDuration(5, TimeUnit.SECONDS))

  implicit val actorSystem = ActorSystem(Behaviors.empty, "test-actor-system")
  implicit val executionContext = actorSystem.executionContext

  val dc = new DeveloperConnection()

  "The service" should {

    "Return all relevant errors when neither user is on Github & Twitter" in {

      val handle1 = "thisUserNotOnGithub"
      val handle2 = "thisUserNeither"

      Get(s"/developers/connected/${handle1}/${handle2}") ~!> dc.route ~> check {
        responseAs[List[String]] should contain allOf (
          s"${handle1} is not a valid user on Github",
          s"${handle2} is not a valid user on Github",
          s"${handle1} is not a valid user on Twitter",
          s"${handle2} is not a valid user on Twitter",
          )
      }
    }

    "Return all relevant errors when one of the users is neither on Github nor Twitter" in {

      val handle1 = "odersky"
      val handle2 = "thisUserInNotHere"

      Get(s"/developers/connected/${handle1}/${handle2}") ~!> dc.route ~> check {
        responseAs[List[String]] should contain allOf (
          s"${handle2} is not a valid user on Twitter",
          s"${handle2} is not a valid user on Github",
        )
      }
    }

    "Return all relevant errors when one of the users is not on Github but has Twitter" in {

      val handle1 = "nebelig"
      val handle2 = "odersky"

      Get(s"/developers/connected/${handle1}/${handle2}") ~!> dc.route ~> check {
        responseAs[List[String]] should equal (List(s"${handle1} is not a valid user on Github"))
      }
    }

    "Return proper result when users exist but are not connected" in {

      val handle1 = "pmichelu"
      val handle2 = "odersky"

      implicit val resultFormat = jsonFormat2(Result)

      Get(s"/developers/connected/${handle1}/${handle2}") ~!> dc.route ~> check {
        responseAs[Result] should equal (Result(false))
      }
    }

    "Return proper result when users exist but are not connected2" in {

      val handle1 = "renatocaval"
      val handle2 = "patriknw"

      implicit val resultFormat = jsonFormat2(Result)

      Get(s"/developers/connected/${handle1}/${handle2}") ~!> dc.route ~> check {
        responseAs[Result] should equal (Result(true, Some(List("akka"))))
      }
    }

  }
}
