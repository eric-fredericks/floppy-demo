package org.kalergic.recipebox.persist

import java.io.{File, FileFilter}
import java.nio.file.{Files, Path, Paths}

import play.api.libs.json.{Format, Json}

import scala.util.{Failure, Success, Try}

trait Dao[T] {
  val filenameExtension = ".dat"

  def all(): Try[Seq[T]]
  def get(id: Long): Try[T]
  def put(id: Long, value: T): Try[Unit]
  def delete(id: Long): Try[Unit]
}

class SimpleDaoImpl[T: Format](storePath: Path) extends Dao[T] {
  require(storePath.toFile.isDirectory, s"Data store path $storePath does not exist.")

  override def all(): Try[Seq[T]] = synchronized {
    storePath.toFile.listFiles(new FileFilter {
      override def accept(file: File): Boolean = Files.isRegularFile(file.toPath)
    }).toSeq.map(_.toPath).map(get).foldLeft(Try(Seq.empty[T])){
      case (Success(seq), Success(next)) => Success(seq :+ next)
      case (Success(_), Failure(e)) => Failure(e)
      case (prevErr, _) => prevErr
    }
  }

  override def get(id: Long): Try[T] = synchronized(get(pathFor(id)))

  private[this] def get(path: Path): Try[T] = synchronized {
    Try {
      Files.readAllBytes(path)
    }.flatMap { data =>
      Try(Json.fromJson(Json.parse(data)).get)
    }
  }

  override def put(id: Long, value: T): Try[Unit] = synchronized {
    Try {
      val data = Json.prettyPrint(Json.toJson[T](value)).getBytes("UTF-8")
      Files.write(pathFor(id), data)
    }
  }

  override def delete(id: Long): Try[Unit] = synchronized(Try(Files.delete(pathFor(id))))

  private[this] def pathFor(id: Long): Path =
    Paths.get(storePath.toString, s"${id.toString}.json")
}
