package org.ciroque.ccr.datastores

import java.util.UUID

import com.mongodb.DBObject
import com.mongodb.casbah.Implicits._
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers
import com.mongodb.casbah.{MongoClient, MongoCollection}
import org.ciroque.ccr.datastores.DataStoreResults.{DataStoreResult, Found, NotFound}
import org.ciroque.ccr.logging.ImplicitLogging._
import org.ciroque.ccr.models.ConfigurationFactory
import org.ciroque.ccr.models.ConfigurationFactory.Configuration
import org.joda.time.DateTime
import org.slf4j.Logger
import spray.json._

import scala.concurrent.Future

object MongoSettingsDataStore {
  val defaultPort = 27017
}

class MongoSettingsDataStore(settings: DataStoreParams)(implicit val logger: Logger) extends SettingsDataStore {

  import com.mongodb.BasicDBList
  import com.mongodb.casbah.commons.MongoDBList
  import org.bson.types.ObjectId
  import spray.json.JsValue

  val client = MongoClient(settings.hostname, settings.port.getOrElse(MongoSettingsDataStore.defaultPort))

  override def upsertConfiguration(configuration: Configuration): Future[DataStoreResult] = {
    val validatedConfiguration = configuration.copy(key = validateKey(configuration.key))
    withImplicitLogging("MongoSettingsDataStore::upsertConfiguration") {
      recordValue("given-configuration", configuration.toJson.toString())
      recordValue("added-configuration", validatedConfiguration.toJson.toString())
      executeInCollection { collection =>
        collection += toMongoDbObject(validatedConfiguration)
        DataStoreResults.Added(validatedConfiguration)
      }
    }
  }

  override def retrieveScopes(environment: String, application: String): Future[DataStoreResult] = {
    withImplicitLogging("MongoSettingsDataStore.retrieveScopes") {
      import org.ciroque.ccr.core.Commons
      recordValue(Commons.KeyStrings.EnvironmentKey, environment)
      recordValue(Commons.KeyStrings.ApplicationKey, application)
      executeInCollection { collection =>
        val results = collection.distinct("key.scope", MongoDBObject("key.environment" -> checkWildcards(environment), "key.application" -> checkWildcards(application)))
        results.map(res => res.asInstanceOf[String]).sortBy(app => app).toList match {
          case Nil => DataStoreResults.NotFound(s"environment '$environment' / application '$application' combination was not found")
          case list: List[String] => DataStoreResults.Found(list.toList)
        }
      }
    }
  }

  override def retrieveSettings(environment: String, application: String, scope: String): Future[DataStoreResult] = {
    withImplicitLogging("MongoSettingsDataStore.retrieveSettings") {
      import org.ciroque.ccr.core.Commons
      recordValue(Commons.KeyStrings.EnvironmentKey, environment)
      recordValue(Commons.KeyStrings.ApplicationKey, application)
      recordValue(Commons.KeyStrings.ScopeKey, scope)
      executeInCollection { collection =>
        val results = collection.distinct(
          "key.setting",
          MongoDBObject(
            "key.environment" -> checkWildcards(environment),
            "key.application" -> checkWildcards(application),
            "key.scope" -> checkWildcards(scope)))
        results.map(res => res.asInstanceOf[String]).sortBy(app => app).toList match {
          case Nil => DataStoreResults.NotFound(s"environment '$environment' / application '$application' / scope '$scope' combination was not found")
          case list: List[String] => DataStoreResults.Found(list.toList)
        }
      }
    }
  }

  override def retrieveApplications(environment: String): Future[DataStoreResult] = {
    withImplicitLogging("MongoSettingsDataStore.retrieveApplications") {
      import org.ciroque.ccr.core.Commons
      recordValue(Commons.KeyStrings.EnvironmentKey, environment)

      executeInCollection { collection =>
        val searchTerm = checkWildcards(environment)
        val results = collection.distinct("key.application", MongoDBObject("key.environment" -> searchTerm))
        results.map(res => res.asInstanceOf[String]).sortBy(app => app).toList match {
          case Nil => DataStoreResults.NotFound(s"environment '$environment' was not found")
          case list: List[String] => DataStoreResults.Found(list.toList)
        }
      }
    }
  }

  override def retrieveEnvironments(): Future[DataStoreResult] = {
    withImplicitLogging("MongoSettingsDataStore.retrieveEnvironments") {
      executeInCollection { collection =>
        val results = collection.distinct("key.environment")
        val environments = results.map(result => result.asInstanceOf[String]).sortBy(environment => environment)

        DataStoreResults.Found(environments.toList)
      }
    }
  }

  override def retrieveConfiguration(environment: String, application: String, scope: String, setting: String, sourceId: Option[String] = None): Future[DataStoreResult] = {
    withImplicitLogging("MongoSettingsDataStore.retrieveConfiguration") {
      import org.ciroque.ccr.core.Commons
      recordValue(Commons.KeyStrings.EnvironmentKey, environment)
      recordValue(Commons.KeyStrings.ApplicationKey, application)
      recordValue(Commons.KeyStrings.ScopeKey, scope)
      recordValue(Commons.KeyStrings.SettingKey, setting)
      recordValue(Commons.KeyStrings.SourceIdKey, sourceId.toString)

      executeInCollection { collection =>
        import com.mongodb.casbah.Imports._
        val environmentQuery = checkWildcards(environment)
        val applicationQuery = checkWildcards(application)
        val scopeQuery = checkWildcards(scope)
        val settingQuery = checkWildcards(setting)
        val configurationQuery = $or(("key.environment" $eq environmentQuery) :: ("key.environment" $eq ConfigurationFactory.DefaultEnvironment)) ++
          $and("key.application" $eq applicationQuery, "key.scope" $eq scopeQuery, "key.setting" $eq settingQuery)
        val dbResult = collection.find(configurationQuery).toList
        dbResult match {
          case Nil => DataStoreResults.NotFound(s"environment '$environment' / application '$application' / scope '$scope' / setting '$setting' combination was not found")
          case list => list.map(fromMongoDbObject).filter(_.isActive) match {
            case Nil => NotFound(s"environment '$environment' / application '$application' / scope '$scope' / setting '$setting' found no active configuration")
            case found: Seq[Configuration] => Found(filterBySourceId(found, sourceId))
          }
        }
      }
    }
  }

  private def executeInCollection(fx: (MongoCollection) => DataStoreResult): Future[DataStoreResult] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val collection = client(settings.database)(settings.catalog)

    Future(fx(collection)).recoverWith {
      case ex => Future.successful(DataStoreResults.Failure("FAILED", ex))
    }
  }

  private def toMongoDbObject(configuration: Configuration) = {
    import org.ciroque.ccr.core.Commons
    RegisterJodaTimeConversionHelpers()

    val coreKeys = List((Commons.KeyStrings.EnvironmentKey, configuration.key.environment)
      , (Commons.KeyStrings.ApplicationKey, configuration.key.application)
      , (Commons.KeyStrings.ScopeKey, configuration.key.scope)
      , (Commons.KeyStrings.SettingKey, configuration.key.setting))

    val keyValues = configuration.key.sourceId match {
      case None => coreKeys
      case Some(sourceId) => coreKeys :+ Commons.KeyStrings.SourceIdKey -> sourceId
    }

    def buildMongoDbObjectGraph(js: JsValue): Any = {
      js match {
        case JsString(s) ⇒ s
        case JsNumber(n) ⇒ n
        case JsBoolean(b) ⇒ b
        case JsObject(m) ⇒ m.map { case (k, v) ⇒ (k, buildMongoDbObjectGraph(v)) }
        case JsArray(e) ⇒ e.map(j ⇒ buildMongoDbObjectGraph(j))
        case JsFalse => "false"
        case JsTrue ⇒ "true"
        case JsNull ⇒ null
      }
    }

    val value = buildMongoDbObjectGraph(configuration.value)

    val mdbo = MongoDBObject(
      Commons.KeyStrings.IdKey -> configuration._id,
      Commons.KeyStrings.KeyKey -> MongoDBObject(keyValues),
      Commons.KeyStrings.ValueKey -> value,
      Commons.KeyStrings.TemporalizationKey -> MongoDBObject(
        Commons.KeyStrings.EffectiveAtKey -> configuration.temporality.effectiveAt,
        Commons.KeyStrings.ExpiresAtKey -> configuration.temporality.expiresAt,
        Commons.KeyStrings.TtlKey -> configuration.temporality.ttl
      )
    )

    mdbo
  }

  private def fromMongoDbObject(dbo: DBObject): Configuration = {
    import com.mongodb.BasicDBList
    import org.ciroque.ccr.core.Commons
    RegisterJodaTimeConversionHelpers() // TODO: Can this be done at a broader scope and less frequently?
    val db = dbo.toMap
    val key = db.get(Commons.KeyStrings.KeyKey).asInstanceOf[DBObject]
    val temporalization = db.get(Commons.KeyStrings.TemporalizationKey).asInstanceOf[DBObject]

    val sourceId = if (key.containsField(Commons.KeyStrings.SourceIdKey))
      Some(key.get(Commons.KeyStrings.SourceIdKey).toString)
    else
      None

    val mongoValue = db.get(Commons.KeyStrings.ValueKey)

    val value = fromMongo(mongoValue)

    ConfigurationFactory(
      UUID.fromString(db.get(Commons.KeyStrings.IdKey).toString),
      key.get(Commons.KeyStrings.EnvironmentKey).toString,
      key.get(Commons.KeyStrings.ApplicationKey).toString,
      key.get(Commons.KeyStrings.ScopeKey).toString,
      key.get(Commons.KeyStrings.SettingKey).toString,
      sourceId,
      value,
      temporalization.get(Commons.KeyStrings.EffectiveAtKey).asInstanceOf[DateTime],
      temporalization.get(Commons.KeyStrings.ExpiresAtKey).asInstanceOf[DateTime],
      temporalization.get(Commons.KeyStrings.TtlKey).asInstanceOf[Long]
    )
  }

  def fromMongoDBObject(obj: MongoDBObject): JsObject = {
    JsObject(obj.toSeq.map { case (key, value) =>
      key -> fromMongo(value)
    }.toMap)
  }

  def fromMongoDBList(list: MongoDBList): JsArray = {
    JsArray(list.map(fromMongo).toVector)
  }

  private def fromMongo(a: Any): JsValue = a match {
    case id: ObjectId => JsString(id.toString)
    case list: BasicDBList => fromMongoDBList(list)
    case obj: DBObject => fromMongoDBObject(obj)
    case long: Long => JsNumber(long)
    case int: Int => JsNumber(int)
    case float: Float => JsNumber(float)
    case double: Double => JsNumber(double)
    case decimal: java.math.BigDecimal => JsNumber(decimal)
    case decimal: scala.BigDecimal => JsNumber(decimal)
    case string: String => JsString(string)
    case boolean: Boolean => JsBoolean(boolean)
    case null => import spray.json.JsNull
      JsNull
  }
}
