package org.kalergic.recipebox.model

import com.sksamuel.avro4s.AvroSchema
import org.apache.avro.Schema
import play.api.libs.json.{Json, OFormat}

case class Quantity(numerator: Int, denominator: Int) {
  private def this(arr: Array[Int]) = this(arr(0), arr(1))
  def this(fraction: String) = this(fraction.split('/').map(_.trim).map(_.toInt))
  def this(numerator: Int) = this(numerator, 1)
}

case object Quantity {
  implicit val format: OFormat[Quantity] = Json.format[Quantity]
}

case class Ingredient(
  quantity: Option[Quantity],
  unit: Option[String],
  description: String
)

object Ingredient {
  implicit val format: OFormat[Ingredient] = Json.format[Ingredient]
}

case class RecipeInfo(
  id: Option[Long],
  title: String,
  description: Option[String],
  category: String
)

object RecipeInfo {
  implicit val format: OFormat[RecipeInfo] = Json.format[RecipeInfo]
}

case class Recipe(
  info: RecipeInfo,
  ingredients: Seq[Ingredient],
  directions: Seq[String]
)

object Recipe {
  implicit val format: OFormat[Recipe] = Json.format[Recipe]

  //ejf-fixMe: needed?
  implicit val schema: Schema = AvroSchema[Recipe]
}

case class RecipeParseError(messages: Seq[String])

object RecipeParseError {
  implicit val format: OFormat[RecipeParseError] = Json.format[RecipeParseError]
}