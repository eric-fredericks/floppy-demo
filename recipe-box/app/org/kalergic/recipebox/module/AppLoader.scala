package org.kalergic.recipebox.module

import java.nio.file.{Files, Path, Paths}

import com.softwaremill.macwire._
import com.softwaremill.tagging._
import controllers.AssetsComponents
import org.kalergic.floppyears.wiretap.ExecutionContextTags.FloppyEarsEC
import org.kalergic.floppyears.wiretap.{ExecutionContextTags, FloppyEarsClient}
import org.kalergic.recipebox.controller.{RecipeController, RecipeControllerImpl}
import org.kalergic.recipebox.model.Recipe
import org.kalergic.recipebox.persist.{Dao, SimpleDaoImpl}
import org.kalergic.recipebox.service.{RecipeService, RecipeServiceImpl}
import play.api.ApplicationLoader.Context
import play.api._
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.DefaultControllerComponents
import play.api.routing.Router
import play.filters.HttpFiltersComponents
import router.Routes

import scala.concurrent.{ExecutionContext, Future}

class AppLoader extends ApplicationLoader {
  override def load(context: Context): Application = {
    LoggerConfigurator(context.environment.classLoader).foreach { cfg =>
      cfg.configure(context.environment)
    }
    new AppComponents(context).application
  }
}

class AppComponents(context: Context) extends BuiltInComponentsFromContext(context) with AhcWSComponents with AssetsComponents with HttpFiltersComponents {

  private val log = Logger(this.getClass)

  val dataHome: Path = Paths.get(System.getProperty("user.dir"), "datastore")

  override lazy val controllerComponents: DefaultControllerComponents = wire[DefaultControllerComponents]
  lazy val prefix: String = "/"
  override lazy val router: Router = wire[Routes]

  lazy val recipeService: RecipeService = wire[RecipeServiceImpl]
  lazy val floppyEarsClient: FloppyEarsClient = {
    val floppyEarsBaseUrl = "http://localhost:8080"

    //ejf-fixMe: convenience method on FloppyEarsActionFunction object.
    val dispatchers = actorSystem.dispatchers
    wire[FloppyEarsClient]
  }
  implicit lazy val myFloppyEarsContext: MyFloppyEarsContext = wire[MyFloppyEarsContext]
  lazy val recipeController: RecipeController = wire[RecipeControllerImpl]
  lazy val recipeDao: Dao[Recipe] = {
    require(Files.isDirectory(dataHome), s"Cannot access data store: ${dataHome.toString}")
    val path = Paths.get(dataHome.toString, "recipes")
    path.toFile.mkdirs
    wire[SimpleDaoImpl[Recipe]]
  }

  val onStart: Unit = {
    log.info("The app is about to start")
  }

  applicationLifecycle.addStopHook { () =>
    log.info("The app is about to stop")
    Future.successful(())
  }
}
