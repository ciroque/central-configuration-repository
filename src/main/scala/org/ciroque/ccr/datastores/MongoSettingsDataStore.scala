package org.ciroque.ccr.datastores

import java.util.UUID

import com.mongodb.DBObject
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers
import com.mongodb.casbah.{MongoClient, MongoCollection}
import org.ciroque.ccr.datastores.DataStoreResults.{DataStoreResult, Found, NotFound}
import org.ciroque.ccr.logging.ImplicitLogging._
import org.ciroque.ccr.models.ConfigurationFactory
import org.ciroque.ccr.models.ConfigurationFactory.Configuration
import org.joda.time.DateTime
import org.slf4j.Logger

import scala.concurrent.Future

class MongoSettingsDataStore(settings: DataStoreProperties)(override implicit val logger: Logger) extends SettingsDataStore {
  val client = MongoClient(settings.hostname, settings.port)

  override def upsertConfiguration(configuration: Configuration): Future[DataStoreResult] = {
    withImplicitLogging("MongoSettingsDataStore::upsertConfiguration") {
      recordValue("added-configuration", configuration.toJson.toString())
      executeInCollection { collection =>
        collection += toMongoDbObject(configuration)
        DataStoreResults.Added(configuration)
      }
    }
  }

  override def retrieveScopes(environment: String, application: String): Future[DataStoreResult] = {
    withImplicitLogging("MongoSettingsDataStore.retrieveScopes") {
      import org.ciroque.ccr.core.Commons
      recordValue(Commons.KeyStrings.environmentKey, environment)
      recordValue(Commons.KeyStrings.applicationKey, application)
      executeInCollection { collection =>
        val results = collection.distinct("key.scope", MongoDBObject("key.environment" -> environment, "key.application" -> application))
        results.map(res => res.asInstanceOf[String]).sortBy(app => app).toList match {
          case Nil => DataStoreResults.NotFound(s"environment '$environment' / application '$application' combination was not found")
          case list: List[String] => DataStoreResults.Found(list)
        }
      }
    }
  }

  override def retrieveSettings(environment: String, application: String, scope: String): Future[DataStoreResult] = {
    withImplicitLogging("MongoSettingsDataStore.retrieveSettings") {
      import org.ciroque.ccr.core.Commons
      recordValue(Commons.KeyStrings.environmentKey, environment)
      recordValue(Commons.KeyStrings.applicationKey, application)
      recordValue(Commons.KeyStrings.scopeKey, scope)
      executeInCollection { collection =>
        val results = collection.distinct(
          "key.setting",
          MongoDBObject(
            "key.environment" -> environment,
            "key.application" -> application,
            "key.scope" -> scope))
        results.map(res => res.asInstanceOf[String]).sortBy(app => app).toList match {
          case Nil => DataStoreResults.NotFound(s"environment '$environment' / application '$application' / scope '$scope' combination was not found")
          case list: List[String] => DataStoreResults.Found(list)
        }
      }
    }
  }

  override def retrieveApplications(environment: String): Future[DataStoreResult] = {
    withImplicitLogging("MongoSettingsDataStore.retrieveApplications") {
      import org.ciroque.ccr.core.Commons
      recordValue(Commons.KeyStrings.environmentKey, environment)

      executeInCollection { collection =>
        val results = collection.distinct("key.application", MongoDBObject("key.environment" -> environment))
        results.map(res => res.asInstanceOf[String]).sortBy(app => app).toList match {
          case Nil => DataStoreResults.NotFound(s"environment '$environment' was not found")
          case list: List[String] => DataStoreResults.Found(list)
        }
      }
    }
  }

  override def retrieveEnvironments(): Future[DataStoreResult] = {
    withImplicitLogging("MongoSettingsDataStore.retrieveEnvironments") {
      executeInCollection { collection =>
        val results = collection.distinct("key.environment")
        val environments = results.map(result => result.asInstanceOf[String]).sortBy(environment => environment)

        DataStoreResults.Found(environments)
      }
    }
  }

  override def retrieveConfiguration(environment: String, application: String, scope: String, setting: String): Future[DataStoreResult] = {
    withImplicitLogging("MongoSettingsDataStore.retrieveConfiguration") {
      import org.ciroque.ccr.core.Commons
      recordValue(Commons.KeyStrings.environmentKey, environment)
      recordValue(Commons.KeyStrings.applicationKey, application)
      recordValue(Commons.KeyStrings.scopeKey, scope)
      recordValue(Commons.KeyStrings.settingKey, setting)
      executeInCollection { collection =>
        import com.mongodb.casbah.Imports._

        val query = $or(("key.environment" $eq environment) :: ("key.environment" $eq ConfigurationFactory.DefaultEnvironment)) ++
          $and("key.application" $eq application, "key.scope" $eq scope, "key.setting" $eq setting)
        collection.find(query).toList match {
          case Nil => DataStoreResults.NotFound(s"environment '$environment' / application '$application' / scope '$scope' / setting '$setting' combination was not found")
          case list => list.map(fromMongoDbObject).filter(_.isActive) match {
            case Nil => NotFound(s"environment '$environment' / application '$application' / scope '$scope' / setting '$setting' found no active configuration")
            case found: Seq[Configuration] => Found(found)
          }
        }
      }
    }
  }

  private def executeInCollection(fx: (MongoCollection) => DataStoreResult): Future[DataStoreResult] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val collection = MongoClient(settings.hostname, settings.port)(settings.databaseName)(settings.catalog)

    Future(fx(collection)).recoverWith {
      case ex => Future.successful(DataStoreResults.Failure("FAILED", ex))
    }
  }

  private def toMongoDbObject(configuration: Configuration) = {
    import org.ciroque.ccr.core.Commons
    RegisterJodaTimeConversionHelpers()
    MongoDBObject(
      Commons.KeyStrings.idKey -> configuration._id, // TODO: Convert to BSON ObjectId
      Commons.KeyStrings.keyKey -> MongoDBObject(
        Commons.KeyStrings.environmentKey -> configuration.key.environment,
        Commons.KeyStrings.applicationKey -> configuration.key.application,
        Commons.KeyStrings.scopeKey -> configuration.key.scope,
        Commons.KeyStrings.settingKey -> configuration.key.setting
      ),
      Commons.KeyStrings.valueKey -> configuration.value,
      Commons.KeyStrings.temporalizationKey -> MongoDBObject(
        Commons.KeyStrings.effectiveAtKey -> configuration.temporality.effectiveAt,
        Commons.KeyStrings.expiresAtKey -> configuration.temporality.expiresAt,
        Commons.KeyStrings.ttlKey -> configuration.temporality.ttl
      )
    )
  }

  private def fromMongoDbObject(dbo: DBObject): Configuration = {
    import org.ciroque.ccr.core.Commons
    RegisterJodaTimeConversionHelpers()
    val db = dbo.toMap
    val key = db.get(Commons.KeyStrings.keyKey).asInstanceOf[DBObject]
    val temporalization = db.get(Commons.KeyStrings.temporalizationKey).asInstanceOf[DBObject]

    ConfigurationFactory(
      UUID.fromString(db.get(Commons.KeyStrings.idKey).toString),
      key.get(Commons.KeyStrings.environmentKey).toString,
      key.get(Commons.KeyStrings.applicationKey).toString,
      key.get(Commons.KeyStrings.scopeKey).toString,
      key.get(Commons.KeyStrings.settingKey).toString,
      db.get(Commons.KeyStrings.valueKey).toString,
      temporalization.get(Commons.KeyStrings.effectiveAtKey).asInstanceOf[DateTime],
      temporalization.get(Commons.KeyStrings.expiresAtKey).asInstanceOf[DateTime],
      temporalization.get(Commons.KeyStrings.ttlKey).asInstanceOf[Long]
    )
  }
}
