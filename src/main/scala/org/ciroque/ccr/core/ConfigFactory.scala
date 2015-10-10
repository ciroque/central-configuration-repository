package org.ciroque.ccr.core

import com.typesafe.config.{Config, ConfigFactory => TSConfigFactory}
import org.ciroque.ccr.datastores.{DataStoreConfig, DataStoreParams}

object ConfigFactory {
  val enginePathPrefix = "ccr.engines"
  val dataStorageClassPath = enginePathPrefix + ".datastore.class"
  val dataStorageParamsPath = enginePathPrefix + ".datastore.params"

  def load(filename: String): DataStoreConfig = {
    val tsconfig = TSConfigFactory.load(filename)
    val clazz = getClazz(tsconfig)
    val params = getParams(tsconfig)
    DataStoreConfig(clazz, params)
  }

  private def getClazz(tsconfig: Config): Option[String] = {
    tsconfig.hasPath(dataStorageClassPath) match {
      case true => Some(tsconfig.getString(dataStorageClassPath))
      case false => None
    }
  }

  private def getParams(config: Config): DataStoreParams = {
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
      val defaultConfig = TSConfigFactory.parseString(defaultConfigString)
      val params = if (config.hasPath(ccrKey)) {
        val ccrConfig = config.getConfig(ccrKey)
        if (ccrConfig.hasPath(enginesKey)) {
          val enginesConfig = ccrConfig.getConfig(enginesKey)
          if (enginesConfig.hasPath(datastoreKey)) {
            val datastoreConfig = enginesConfig.getConfig(datastoreKey)
            if (datastoreConfig.hasPath(paramsKey)) {
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
      if (realizedConfig.hasPath(key)) realizedConfig.getString(key)
      else null
    }

    def getValueOption(key: String): Option[Int] = {
      if (realizedConfig.hasPath(key)) Some(realizedConfig.getInt(key))
      else None
    }

    DataStoreParams(
      realizedConfig.getString(hostnameKey),
      getValueOption(portKey),
      realizedConfig.getString(databaseKey),
      realizedConfig.getString(catalogKey),
      getValueOrNull(usernameKey),
      getValueOrNull(passwordKey)
    )
  }
}
