package org.ciroque.ccr.datastores

import java.util.UUID

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers
import com.mongodb.casbah.{MongoClient, MongoCollection}
import com.mongodb.{BasicDBObject, DBObject}
import org.ciroque.ccr.core.Commons
import org.ciroque.ccr.datastores.DataStoreResults.{DataStoreResult, Deleted, Found, NotFound}
import org.ciroque.ccr.logging.ImplicitLogging._
import org.ciroque.ccr.models.ConfigurationFactory
import org.ciroque.ccr.models.ConfigurationFactory.Configuration
import org.joda.time.DateTime
import org.slf4j.Logger
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object MongoSettingsDataStore {
  val defaultPort = 27017
}

class MongoSettingsDataStore(settings: DataStoreParams)(implicit val logger: Logger) extends SettingsDataStore {

  import com.mongodb.BasicDBList
  import com.mongodb.casbah.commons.MongoDBList
  import org.bson.types.ObjectId
  import spray.json.JsValue

  def collection(database: String)(collection: String) =
    MongoClient(settings.hostname, settings.port.getOrElse(MongoSettingsDataStore.defaultPort))(database)(collection)

  override def deleteConfiguration(configuration: Configuration): Future[DataStoreResult] = Future.successful(Deleted(configuration))

  override def insertConfiguration(configuration: Configuration): Future[DataStoreResult] = {
    val validatedConfiguration = configuration.copy(key = validateKey(configuration.key))
    withImplicitLogging("MongoSettingsDataStore::insertConfiguration") {
      recordValue("given-configuration", configuration.toJson.toString())
      recordValue("added-configuration", validatedConfiguration.toJson.toString())
      executeInCollection { collection =>
        collection.insert(toMongoDbObject(validatedConfiguration))
        val dsr = DataStoreResults.Added(validatedConfiguration)
        insertAuditRecord(DateTime.now, validatedConfiguration, None).recoverWith {
          case t: Throwable =>
            recordValue("RecordAuditFailure::INSERT", t.toString)
            Future.successful(DataStoreResults.Errored(validatedConfiguration, t.getMessage))
        }
        dsr
      }.recoverWith {
        case t: Throwable =>
          setResultException(t)
          Future.successful(DataStoreResults.Errored(validatedConfiguration, Commons.DatastoreErrorMessages.DuplicateKeyError))
      }
    }
  }

  override def updateConfiguration(configuration: Configuration): Future[DataStoreResult] = {
    val validatedConfiguration = configuration.copy(key = validateKey(configuration.key))
    withImplicitLogging("MongoSettingsDataStore::updateConfiguration") {
      recordValue("original-configuration", configuration.toJson.toString())
      recordValue("validated-configuration", validatedConfiguration.toJson.toString())
      val queryDoc = new BasicDBObject("_id", validatedConfiguration._id)
      executeInCollection { collection =>
        collection.findAndModify(queryDoc, toMongoDbObject(validatedConfiguration)) match {
          case Some(foundDocument) =>
            val previousConfiguration = fromMongoDbObject(foundDocument)
            val dsr = DataStoreResults.Updated(previousConfiguration, validatedConfiguration)
            insertAuditRecord(DateTime.now, previousConfiguration, Some(validatedConfiguration)).recoverWith {
              case t: Throwable =>
                recordValue("RecordAuditFailure::UPDATE", t.toString)
                Future.successful(DataStoreResults.Errored(validatedConfiguration, t.getMessage))
            }
            dsr
          case None => DataStoreResults.NotFound(Some(validatedConfiguration), Commons.DatastoreErrorMessages.NotFoundError)
        }
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
          case Nil => DataStoreResults.NotFound(None, s"environment '$environment' / application '$application' combination was not found")
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
          case Nil => DataStoreResults.NotFound(None, s"environment '$environment' / application '$application' / scope '$scope' combination was not found")
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
          case Nil => DataStoreResults.NotFound(None, s"environment '$environment' was not found")
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

      queryConfigurations(environment, application, scope, setting) flatMap {
        configurations =>
          val dataStoreResult = configurations match {
            case Nil => NotFound(None, s"environment '$environment' / application '$application' / scope '$scope' / setting '$setting' combination was not found")
            case list => list.filter(_.isActive) match {
              case Nil  => NotFound(None, s"environment '$environment' / application '$application' / scope '$scope' / setting '$setting' found no active configuration")
              case found: Seq[Configuration] => Found(filterBySourceId(found, sourceId))
            }
          }

          Future.successful(dataStoreResult)
      }
    }
  }

  override def retrieveConfigurationSchedule(environment: String, application: String, scope: String, setting: String): Future[DataStoreResult] = {
    import org.ciroque.ccr.models.ConfigurationFactory.ConfigurationOrdering._
    withImplicitLogging("MongoSettingsDataStore.retrieveConfigurationSchedule") {
      recordValue(Commons.KeyStrings.EnvironmentKey, environment)
      recordValue(Commons.KeyStrings.ApplicationKey, application)
      recordValue(Commons.KeyStrings.ScopeKey, scope)
      recordValue(Commons.KeyStrings.SettingKey, setting)

      queryConfigurations(environment, application, scope, setting) flatMap {
        configurations =>
          val dataStoreResult = configurations match {
            case Nil => NotFound(None, s"environment '$environment' / application '$application' / scope '$scope' / setting '$setting' combination was not found")
            case list => Found(list.sortBy(c => c))
            }

          Future.successful(dataStoreResult)
        }
      }
  }

  private def insertAuditRecord(when: DateTime, original: Configuration, updated: Option[Configuration]): Future[DataStoreResult] = {
    Future {
      val queryDoc = MongoDBObject("_id" -> original._id)
      val baseAuditEntry = List(("date", when), ("original", toMongoDbObject(original)))
      val auditEntry = updated match {
        case None => baseAuditEntry
        case Some(config) => baseAuditEntry :+ ("updated", toMongoDbObject(config))
      }
      val updateDoc = MongoDBObject("$push" -> MongoDBObject("history" -> MongoDBObject(auditEntry)))
      val auditCollection = collection(settings.database)(settings.auditCatalog)

      val result = auditCollection.update(queryDoc, updateDoc, true)
      if(result.isUpdateOfExisting) {
        DataStoreResults.Updated(original, updated.getOrElse(ConfigurationFactory.EmptyConfiguration))
      } else {
        DataStoreResults.Added(original)
      }
    }
  }

  private def queryConfigurations(environment: String, application: String, scope: String, setting: String): Future[List[Configuration]] = {
    val configurationQuery = buildConfigurationQuery(environment, application, scope, setting)
    executeInCollection { collection =>
      collection.find(configurationQuery).toList match {
        case Nil => Nil
        case list => list.map(fromMongoDbObject)
      }
    }
  }

  private def buildConfigurationQuery(environment: String, application: String, scope: String, setting: String) = {
    val environmentQuery = checkWildcards(environment)
    val applicationQuery = checkWildcards(application)
    val scopeQuery = checkWildcards(scope)
    val settingQuery = checkWildcards(setting)
    val assQuery = $and("key.application" $eq applicationQuery, "key.scope" $eq scopeQuery, "key.setting" $eq settingQuery)
    $or(("key.environment" $eq environmentQuery) :: ("key.environment" $eq ConfigurationFactory.DefaultEnvironment)) ++ assQuery
  }

  private def executeInCollection[T](fx: (MongoCollection) => T): Future[T] = {
    Future { fx(collection(settings.database)(settings.catalog)) }
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
        case JsNumber(n) ⇒ n.doubleValue()
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
    case uuid: UUID => JsString(uuid.toString)
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
    case null => JsNull
    case jdt: DateTime => JsString(jdt.toString)
  }

  override def retrieveAuditHistory(uuid: UUID): Future[DataStoreResult] =
    Future.successful(DataStoreResults.Found(List()))
}
