package org.ciroque.ccr.datastores

import com.typesafe.config.{ConfigFactory, Config}

object DataStoreProperties {
  def fromConfig(config: Config): DataStoreProperties = {

    val ccrKey = "ccr"
    val enginesKey = "engines"
    val datastoreKey = "datastore"
    val paramsKey = "params"

    val hostnameKey = "hostname"
    val portKey = "port"
    val databaseKey = "database"
    val catalogKey = "catalog"
    val usernameKey = "username"
    val passwordKey = "password"

    // this is abso-freaking-lutely nasty. The Typesafe Config implementation is NOT functional friendly
    // at least it is all in one place if the underlying configuration library is changed
    def getParamsConfig: Config = {
      val defaultConfigString = s"""$hostnameKey"": ""localhost"",""$databaseKey"": ""ccr"",""$catalogKey"": ""settings"""
      val defaultConfig = ConfigFactory.parseString(defaultConfigString)
      val params = if(config.hasPath(ccrKey)) {
        val ccrConfig = config.getConfig(ccrKey)
        if(ccrConfig.hasPath(enginesKey)) {
          val enginesConfig = ccrConfig.getConfig(enginesKey)
          if(enginesConfig.hasPath(datastoreKey)) {
            val datastoreConfig = enginesConfig.getConfig(datastoreKey)
            if(datastoreConfig.hasPath(paramsKey)) {
              Some(datastoreConfig.getConfig(paramsKey))
            } else
              None
          } else
            None
        } else
          None
      } else
        None

      params match {
        case Some(givenConfig) => givenConfig.withFallback(defaultConfig).resolve()
        case None => defaultConfig
      }
    }

    val realizedConfig = getParamsConfig

    def getValueOrNull(key: String): String = {
      if(realizedConfig.hasPath(key)) realizedConfig.getString(key)
      else null
    }

    def getValueOption(key: String): Option[Int] = {
      if(realizedConfig.hasPath(key)) Some(realizedConfig.getInt(key))
      else None
    }

    DataStoreProperties(
      realizedConfig.getString(hostnameKey),
      getValueOption(portKey),
      realizedConfig.getString(databaseKey),
      realizedConfig.getString(catalogKey),
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
