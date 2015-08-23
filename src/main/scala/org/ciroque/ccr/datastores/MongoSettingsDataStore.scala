package org.ciroque.ccr.datastores

import java.util.UUID

import com.mongodb.DBObject
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers
import com.mongodb.casbah.{MongoClient, MongoCollection}
import org.ciroque.ccr.datastores.DataStoreResults.{DataStoreResult, Found, NotFound}
import org.ciroque.ccr.models.ConfigurationFactory
import org.ciroque.ccr.models.ConfigurationFactory.Configuration
import org.joda.time.DateTime

import scala.concurrent.Future

class MongoSettingsDataStore(settings: DataStoreProperties) extends SettingsDataStore {
  val client = MongoClient(settings.hostname, settings.port)

  override def upsertConfiguration(configuration: Configuration): Future[DataStoreResult] = {
    executeInCollection { collection =>
      collection += toMongoDbObject(configuration)
      DataStoreResults.Added(configuration)
    }
  }

  override def retrieveScopes(environment: String, application: String): Future[DataStoreResult] = {
    executeInCollection { collection =>
      val results = collection.distinct("key.scope", MongoDBObject("key.environment" -> environment, "key.application" -> application))
      results.map(res => res.asInstanceOf[String]).sortBy(app => app).toList match {
        case Nil => DataStoreResults.NotFound(s"environment '$environment' / application '$application' combination was not found")
        case list: List[String] => DataStoreResults.Found(list)
      }
    }
  }

  override def retrieveSettings(environment: String, application: String, scope: String): Future[DataStoreResult] = {
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

  override def retrieveApplications(environment: String): Future[DataStoreResult] = {
    executeInCollection { collection =>
      val results = collection.distinct("key.application", MongoDBObject("key.environment" -> environment))
      results.map(res => res.asInstanceOf[String]).sortBy(app => app).toList match {
        case Nil => DataStoreResults.NotFound(s"environment '$environment' was not found")
        case list: List[String] => DataStoreResults.Found(list)
      }
    }
  }

  override def retrieveEnvironments(): Future[DataStoreResult] = {
    executeInCollection { collection =>
      val results = collection.distinct("key.environment")
      val environments = results.map(result => result.asInstanceOf[String]).sortBy(environment => environment)

      DataStoreResults.Found(environments)
    }
  }

  override def retrieveConfiguration(environment: String, application: String, scope: String, setting: String): Future[DataStoreResult] = {
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

  private def executeInCollection(fx: (MongoCollection) => DataStoreResult): Future[DataStoreResult] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val collection = MongoClient(settings.hostname, settings.port)(settings.databaseName)(settings.catalog)

    Future(fx(collection)).recoverWith {
      case ex => Future.successful(DataStoreResults.Failure("FAILED", ex))
    }
  }

  private def toMongoDbObject(configuration: Configuration) = {
    RegisterJodaTimeConversionHelpers()
    MongoDBObject(
      "_id" -> configuration._id, // TODO: Convert to BSON ObjectId
      "key" -> MongoDBObject(
        "environment" -> configuration.key.environment,
        "application" -> configuration.key.application,
        "scope" -> configuration.key.scope,
        "setting" -> configuration.key.setting
      ),
      "value" -> configuration.value,
      "temporalization" -> MongoDBObject(
        "effectiveAt" -> configuration.temporality.effectiveAt,
        "expiresAt" -> configuration.temporality.expiresAt,
        "ttl" -> configuration.temporality.ttl
      )
    )
  }

  private def fromMongoDbObject(dbo: DBObject): Configuration = {
    RegisterJodaTimeConversionHelpers()
    val db = dbo.toMap
    val key = db.get("key").asInstanceOf[DBObject]
    val temporalization = db.get("temporalization").asInstanceOf[DBObject]

    ConfigurationFactory(
      UUID.fromString(db.get("_id").toString),
      key.get("environment").toString,
      key.get("application").toString,
      key.get("scope").toString,
      key.get("setting").toString,
      db.get("value").toString,
      temporalization.get("effectiveAt").asInstanceOf[DateTime],
      temporalization.get("expiresAt").asInstanceOf[DateTime],
      temporalization.get("ttl").asInstanceOf[Long]
    )
  }
}
