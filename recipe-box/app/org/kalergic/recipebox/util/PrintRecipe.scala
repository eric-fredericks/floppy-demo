package org.kalergic.recipebox.util

import org.kalergic.recipebox.model.{Ingredient, Quantity, Recipe, RecipeInfo}
import play.api.libs.json.Json

object PrintRecipe extends App {
  val info = RecipeInfo(
    id = None,
    title = "Pancakes",
    description = None,
    category = "Breakfast"
  )

  val ingredients = Seq(
    Ingredient(Some(new Quantity("3/4")), Some("cup"), "flour"),
    Ingredient(Some(new Quantity(1)), Some("tbsp"), "sugar"),
    Ingredient(Some(new Quantity("1/2")), Some("tbsp"), "baking powder"),
    Ingredient(None, Some("A few sprinkles"), "cinnamon"),
    Ingredient(Some(new Quantity(1)), None, "egg"),
    Ingredient(Some(new Quantity(2)), Some("tbsp"), "cooking oil"),
    Ingredient(Some(new Quantity("3/4")), Some("cup"), "skim milk"),
    Ingredient(Some(new Quantity("1/4")), Some("tsp"), "vanilla extract")
  )
  val directions = Seq(
    "Turn oven on to Warm setting (180 degrees).",
    "Mix dry ingredients in a medium bowl.",
    "Beat egg in a small bowl.",
    "Add liquid ingredients to egg and mix well.",
    "Combine liquid ingredients with dry ingredients and stir with a fork until just mixed, scraping the bottom of the bowl.",
    "Spray large square pan (or melt butter in pan).",
    "When pan is hot, drop 1/4 cup of batter on pan per pancake.",
    "When bubbles begin to form, flip pancakes. Watch closely.",
    "Transfer each pancake to warm oven to keep warm."
  )
  val recipe = Recipe(info, ingredients, directions)

  println(Json.prettyPrint(Json.toJson(recipe)))
}
