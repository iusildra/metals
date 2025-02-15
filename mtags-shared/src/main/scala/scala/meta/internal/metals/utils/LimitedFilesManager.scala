package scala.meta.internal.metals.utils

import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class LimitedFilesManager(
    directory: Path,
    fileLimit: Int,
    prefixPattern: String
) {
  private val fileNameRegex = s"${prefixPattern}([-+]?[0-9]+)".r

  def getAllFiles(): List[TimestampedFile] = {
    if (Files.exists(directory) && Files.isDirectory(directory)) {
      directory.toFile.listFiles().flatMap(timestampedFile).toList
    } else List()
  }

  def deleteOld(limit: Int = fileLimit): List[TimestampedFile] = {
    val files = getAllFiles()
    if (files.length > limit) {
      val filesToDelete = files
        .sortBy(_.timestamp)
        .slice(0, files.length - limit)
      filesToDelete.foreach { f => Files.delete(f.toPath) }
      filesToDelete
    } else List()
  }

  private def timestampedFile(file: File): Option[TimestampedFile] = {
    file.getName() match {
      case fileNameRegex(time) => Some(TimestampedFile(file, time.toLong))
      case _: String => None
    }
  }
}

case class TimestampedFile(file: File, timestamp: Long) {
  def toPath: Path = file.toPath()
  def name: String = file.getName()
}
