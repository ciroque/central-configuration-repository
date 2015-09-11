package org.ciroque.ccr.datastores

import com.typesafe.config.Config

object DataStoreProperties {
  def fromConfig(config: Config): DataStoreProperties = {
    DataStoreProperties(
      config.getString("hostname"),
      config.getInt("port"),
      config.getString("database"),
      config.getString("catalog"),
      config.atKey("username").toString,
      config.atKey("password").toString
    )
  }
}

case class DataStoreProperties(hostname: String,
                               port: Int,
                               database: String,
                               catalog: String,
                               username: String,
                               password: String)


