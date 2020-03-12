package org.kalergic.recipebox.service

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

import org.kalergic.recipebox.model.{Recipe, RecipeInfo}
import org.kalergic.recipebox.persist.Dao
import org.kalergic.recipebox.util.StopWords

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success}

trait RecipeService {
  def getRecipe(id: Long): Future[Option[Recipe]]
  def saveRecipe(recipe: Recipe): Future[Recipe]
  def search(keywords: Seq[String]): Future[Seq[RecipeInfo]]
}

object RecipeService {
  import StopWords._

  private[service] case class RecipeRecord(
      recipe: Recipe,
      keywords: Set[String]
  )

  implicit class KeywordBuilder(recipe: Recipe) {
    def keywords: Set[String] =
      ((recipe.info.title +:
        recipe.info.description.toSeq :+
        recipe.info.category) ++
        recipe.ingredients.map(_.description) ++
        recipe.directions)
        .flatMap(_.split(Array(' ', '\t', '\n', '\r')))
        .map(_.toLowerCase)
        .filter(isStopWord)
        .toSet
  }
}

class RecipeServiceImpl(recipeDao: Dao[Recipe])(implicit ec: ExecutionContext)
    extends RecipeService {
  import RecipeService._

  private[this] val nextId = new AtomicLong(1)
  private[this] val cache = new ConcurrentHashMap[Long, RecipeRecord]

  recipeDao.all() match {
    case Success(rcs) =>
      val maxId = (rcs.map { recipe =>
        cache.put(recipe.info.id.get, RecipeRecord(recipe, recipe.keywords))
        recipe.info.id.get
      } :+ 0L).max
      nextId.set(maxId + 1)
    case Failure(e) => throw e
  }

  override def getRecipe(id: Long): Future[Option[Recipe]] =
    Future.successful(Option(cache.get(id)).map(_.recipe))

  override def saveRecipe(recipe: Recipe): Future[Recipe] = Future {
    val id = recipe.info.id.getOrElse(nextId.getAndIncrement())
    val updatedInfo: RecipeInfo = recipe.info.copy(id = Option(id))
    val updatedRecipe: Recipe = recipe.copy(info = updatedInfo)
    val record =
      RecipeRecord(recipe = updatedRecipe, keywords = recipe.keywords)
    recipeDao
      .synchronized {
        recipeDao.put(id, updatedRecipe).map { _ =>
          cache.put(id, record)
          updatedRecipe
        }
      }
      .getOrElse(throw new Exception("Internal Server Error"))
  }

  override def search(keywords: Seq[String]): Future[Seq[RecipeInfo]] =
    Future.successful(
      cache.values.asScala.toSeq
        .filter { recipe =>
          keywords.map(_.toLowerCase).forall(recipe.keywords.contains)
        }
        .map(_.recipe.info)
    )
}
