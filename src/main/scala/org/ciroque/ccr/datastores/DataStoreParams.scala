package org.ciroque.ccr.datastores

case class DataStoreParams(hostname: String,
                           port: Option[Int],
                           database: String,
                           catalog: String,
                           username: String,
                           password: String,
                          auditCatalog: String = "auditing")

case class DataStoreConfig(clazz: Option[String], params: DataStoreParams)