package lt.ezz

sealed abstract class Error {
  def errorMessage: String
}
final case class TwitterUserDoesNotExist(handle: String) extends Error {
  def errorMessage = s"${handle} is not a valid user on Twitter"
}
final case class GithubUserDoesNotExist(handle: String) extends Error {
  def errorMessage = s"${handle} is not a valid user on Github"
}

case class Result(connected: Boolean, organizations: Option[List[String]] = None)
case class Organization(login: String, id: Long)
case class GithubUser(login: String, id: Long)