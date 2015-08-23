package org.ciroque.ccr.datastores


case class DataStoreProperties(hostname: String,
                               port: Int,
                               databaseName: String,
                               catalog: String,
                               username: String,
                               password: String)


