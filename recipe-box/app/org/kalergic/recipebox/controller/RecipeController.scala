package org.kalergic.recipebox.controller

import controllers.Assets
import org.kalergic.floppyears.wiretap.Annotations._
import org.kalergic.floppyears.wiretap._
import org.kalergic.recipebox.model.{Recipe, RecipeInfo, RecipeParseError}
import org.kalergic.recipebox.service.RecipeService
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

object RecipeController {
  val toRecipeInfo: Recipe => RecipeInfo = r => r.info
}

trait RecipeController {

  import RecipeController._

  @Wiretap(actionName = "GetRecipe", majorVersion = 1)
  @WiretapResponse(classOf[Recipe])
  def getRecipe(id: Long): EssentialAction

  @Wiretap(actionName = "SaveRecipe", majorVersion = 1)
  @WiretapRequest(classOf[Recipe])
  @WiretapResponse(classOf[Recipe])
  @WiretapResponseTransform(classOf[RecipeInfo], toRecipeInfo)
  def saveRecipe(): EssentialAction

  @Wiretap(actionName = "RecipeSearch", majorVersion = 1)
  @WiretapResponse(classOf[Seq[RecipeInfo]])
  def search(q: Seq[String]): EssentialAction
}

class RecipeControllerImpl(
    components: ControllerComponents,
    assets: Assets,
    recipeService: RecipeService
)(implicit floppyContext: FloppyEarsContext[Request], ec: ExecutionContext)
    extends AbstractController(components)
    with RecipeController {

  import FloppyEarsActionFunction._
  import RecipeControllerFloppyEarsSupport._

  private[this] val GetRecipeV1: ActionContext[Request] =
    getRecipeV1Context[Request]
  private[this] val getRecipeAction: ActionBuilder[Request, AnyContent] =
    Action.withFloppyEars(GetRecipeV1)

  private[this] val SaveRecipeV1: ActionContext[Request] =
    saveRecipeV1Context[Request]
  private[this] val saveRecipeAction: ActionBuilder[Request, AnyContent] =
    Action.withFloppyEars(SaveRecipeV1)

  private[this] val RecipeSearchV1: ActionContext[Request] =
    recipeSearchV1Context[Request]
  private[this] val searchAction: ActionBuilder[Request, AnyContent] =
    Action.withFloppyEars(RecipeSearchV1)

  override def getRecipe(id: Long): EssentialAction = getRecipeAction.async {
    recipeService.getRecipe(id).map {
      case Some(recipe) => Ok(Json.toJson(recipe))
      case None         => NotFound
    }
  }

  override def saveRecipe(): EssentialAction =
    saveRecipeAction.async(parse.json) { request =>
      Json.fromJson[Recipe](request.body) match {
        case JsSuccess(recipe, _) =>
          recipeService.saveRecipe(recipe).map(ri => Ok(Json.toJson(ri)))
        case JsError(errors) =>
          Future.successful(
            BadRequest(
              Json.toJson(
                RecipeParseError(errors.flatMap(_._2.flatMap(_.messages)).toSeq)
              )
            )
          )
      }
    }

  override def search(keywords: Seq[String]): EssentialAction =
    searchAction.async {
      recipeService.search(keywords).map {
        case recipeInfos @ _ => Ok(Json.toJson(recipeInfos))
      }
    }
}
