package lt.ezz

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding.Get
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, get, path, _}
import akka.http.scaladsl.unmarshalling.Unmarshal
import cats.data.{NonEmptyChain, ValidatedNec}
import cats.implicits._
import cats.{Semigroup, SemigroupK}
import com.danielasfregola.twitter4s.TwitterRestClient
import spray.json.DefaultJsonProtocol.{jsonFormat2, _}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Try
import scala.language.postfixOps

class DeveloperConnection(implicit executionContext: ExecutionContext, actorSystem: ActorSystem[Nothing]) {

  val twitterRestClient = TwitterRestClient()

  // Needed by Cats.Validated for combining Invalid side of Validated[Error, _]
  implicit val necSemigroup: Semigroup[NonEmptyChain[Error]] =
    SemigroupK[NonEmptyChain].algebra[Error]

  // Needed by Cats.Validated for combining Valid side of Validated[_, Boolean]
  implicit val booleanSemigroup: Semigroup[Boolean] = (x: Boolean, y: Boolean) => x && y

  // for marshaling result into json
  implicit val resultFormat = jsonFormat2(Result)
  // for unmarshalling response from Github
  implicit val organizationFormat = jsonFormat2(Organization)
  // for unmarshalling response from Github
  implicit val userFormat = jsonFormat2(GithubUser)

  val route =
    path("developers" / "connected" / Segment / Segment) { (handle1, handle2) =>
      get {
        complete(
          developerConnection(handle1, handle2).map[ToResponseMarshallable] { f =>
            f match {
              case Right(r) => r
              case Left(e) => e.map(_.errorMessage)
            }
          }
        )
      }
    }

  def checkGithubUserExists(handle: String): Future[ValidatedNec[Error, Boolean]] = {
    val request = Get(s"https://api.github.com/users/${handle}")
    for {
      response <- Http().singleRequest(request)
      user <- if (response.status == StatusCodes.OK)
        Unmarshal(response).to[GithubUser].map{ _ => true.validNec }
      else Future { GithubUserDoesNotExist(handle).invalidNec }
    } yield user
  }

  def getUsersGithubOrganizations(handle: String): Future[List[Organization]] = {
    val request = Get(s"https://api.github.com/users/${handle}/orgs")
    for {
      response <- Http().singleRequest(request)
      orgs <- Unmarshal(response).to[List[Organization]]
    } yield orgs
  }

  def findCommonOrganizations(handle1: String, handle2: String): Future[ValidatedNec[Error, List[Organization]]] = {
    for {
      orgs1 <- getUsersGithubOrganizations(handle1)
      orgs2 <- getUsersGithubOrganizations(handle2)
    } yield orgs1.intersect(orgs2).validNec
  }

  def checkGithubRelationship(handle1: String, handle2: String): Future[ValidatedNec[Error, List[Organization]]] = {
    for {
      u1 <- checkGithubUserExists(handle1)
      u2 <- checkGithubUserExists(handle2)
      r <- if (u1.isValid && u2.isValid) findCommonOrganizations(handle1, handle2) else Future { u1.map(_ => List.empty[Organization]) combine u2.map(_ => List.empty[Organization]) }
    } yield r
  }

  def checkTwitterRelationship(handle1: String, handle2: String): Future[ValidatedNec[Error, Boolean]] = {
    for {
      u1 <- checkTwitterUserExists(handle1)
      u2 <- checkTwitterUserExists(handle2)
      r <- if (u1.isValid && u2.isValid) checkTwitterRelationshipBetweenUsers(handle1, handle2) else Future { u1 combine u2 }
    } yield r
  }

  def checkTwitterUserExists(handle: String): Future[ValidatedNec[Error, Boolean]] = {
      twitterRestClient.searchForUser(handle).map { r =>
        if (r.data.size > 0)
          true.validNec
        else
          TwitterUserDoesNotExist(handle).invalidNec
      }
  }

  def checkTwitterRelationshipBetweenUsers(handle1: String, handle2: String): Future[ValidatedNec[Error, Boolean]] = {
    val relationship = twitterRestClient.relationshipBetweenUsers(handle1, handle2)
    relationship.map { l =>
      if (l.data.relationship.source.following == true && l.data.relationship.target.following == true)
        true.validNec
      else
        false.validNec
    }
  }

  /*
    If both Twitter and Github responses are valid, return Either.Right
    If any of the responses has errors, combine them into a Either.Left
   */
  def checkIfConnected(github: ValidatedNec[Error, List[Organization]], twitter: ValidatedNec[Error, Boolean]): Either[List[Error], Result] = {
    (github.toEither, twitter.toEither) match {
      case (g, t) if g.isRight && t.isRight => {
        val orgs = g.getOrElse(List.empty[Organization]).map(_.login)
        Right(
          Result(
            g.getOrElse(List.empty[Organization]).size > 0 && t.getOrElse(false),
            Option.unless(orgs.isEmpty)(orgs)))
      }
      case (g, t) => Left(
        g.swap.map(_.toList).getOrElse(List.empty[Error]) ++
          t.swap.map(_.toList).getOrElse(List.empty[Error])
      )
    }
  }

  def developerConnection(handle1: String, handle2: String): Future[Either[List[Error], Result]] = {
    for {
      github <- checkGithubRelationship(handle1, handle2)
      twitter <- checkTwitterRelationship(handle1, handle2)
    } yield checkIfConnected(github, twitter)
  }
}
