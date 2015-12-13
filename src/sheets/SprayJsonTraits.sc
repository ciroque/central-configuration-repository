import spray.json._

object Credentials {
  final val UserClazz = "user"
  final val ClientClazz = "client"
}

trait Credentials {
  val username: String
  val password: String
  val clazz: String
}

case class UserCredentials(username: String, password: String, clazz: String = Credentials.UserClazz) extends Credentials

case class ClientCredentials(username: String, password: String, clazz: String = Credentials.ClientClazz) extends Credentials

object CredentialsProtocol extends DefaultJsonProtocol {
  implicit val UserCredentialsFormat = jsonFormat3(UserCredentials)
  implicit val ClientCredentialsFormat = jsonFormat3(ClientCredentials)

  implicit object CredentialsFormat extends JsonFormat[Credentials] {
    override def read(json: JsValue): Credentials =
      json.asJsObject.fields("clazz") match {
        case JsString(Credentials.ClientClazz) => json.asInstanceOf[ClientCredentials]
        case JsString(Credentials.UserClazz) => json.asInstanceOf[UserCredentials]
        case JsString(unknown) => throw new IllegalArgumentException(s"'$unknown' is not a known Credential type")
        case _ => throw new IllegalArgumentException(s"Json type is not supported for Credentials")
      }

    override def write(obj: Credentials): JsValue =
      obj match {
        case user: UserCredentials => user.toJson
        case client: ClientCredentials => client.toJson
      }
  }
}

def printUserTypesAsJson() = {
  import CredentialsProtocol._

  val userCredentials = UserCredentials("username", "userpass")
  val clientCredentials = ClientCredentials("clientname", "clientpass")

  var credentials: Credentials = userCredentials
  println(credentials.toJson.toString())

  credentials = clientCredentials
  println(credentials.toJson.toString())

}
printUserTypesAsJson()
