package org.ciroque.ccr.core

import java.io.Closeable

object Resource {
  def using[A <: Closeable, B](resource: A)(f: A => B): B = {
    require(resource != null, "The supplied resource was null.")
    try {
      f(resource)
    } finally {
      resource.close()
    }
  }
}
