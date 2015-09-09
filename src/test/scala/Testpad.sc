import com.typesafe.config.ConfigFactory

import scala.reflect.io.Directory

val path = Directory.Current

val config = ConfigFactory.load("NoDataStoreEntries.conf")

config
