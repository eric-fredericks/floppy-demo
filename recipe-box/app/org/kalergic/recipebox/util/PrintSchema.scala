package org.kalergic.recipebox.util

import org.apache.avro.Schema
import org.kalergic.recipebox.model.Recipe

object PrintSchema extends App {
  val schemaStr = Recipe.schema.toString(true)

  println(schemaStr)

  val schema = new Schema.Parser().parse(schemaStr)
  println("Equal? " + schema.equals(Recipe.schema))
  println('\u2588')
}
