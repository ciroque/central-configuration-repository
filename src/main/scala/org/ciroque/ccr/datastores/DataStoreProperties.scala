package org.ciroque.ccr.datastores

import com.typesafe.config.{ConfigFactory, Config}

object DataStoreProperties {
  def fromConfig(config: Config): DataStoreProperties = {

    val hostnameKey = "hostname"
    val portKey = "port"
    val databaseKey = "database"
    val catalogKey = "catalog"
    val usernameKey = "username"
    val passwordKey = "password"

    def getValueOrNull(key: String): String = {
      if(config.hasPath(key)) config.getString(key)
      else null
    }

    def getValueOption(key: String): Option[Int] = {
      if(config.hasPath(key)) Some(config.getInt(key))
      else None
    }

    val defaultConfig = ConfigFactory.parseString("params:{\"hostname\": \"localhost\",\"database\": \"ccr\",\"catalog\": \"settings\"}")

    val cfg = config.resolveWith(defaultConfig)

    DataStoreProperties(
      config.getString(hostnameKey),
      getValueOption(portKey),
      config.getString(databaseKey),
      config.getString(catalogKey),
      getValueOrNull(usernameKey),
      getValueOrNull(passwordKey)
    )
  }
}

case class DataStoreProperties(hostname: String,
                               port: Option[Int],
                               database: String,
                               catalog: String,
                               username: String,
                               password: String)


