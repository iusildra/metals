package scala.meta.internal.builds

import java.security.MessageDigest

import scala.concurrent.Future

import scala.meta.internal.bsp.BspConfigGenerationStatus._
import scala.meta.internal.metals.BuildInfo
import scala.meta.internal.metals.MetalsEnrichments._
import scala.meta.internal.metals.UserConfiguration
import scala.meta.internal.metals.scalacli.ScalaCli
import scala.meta.io.AbsolutePath

class ScalaCliBuildTool(
    val workspaceVersion: Option[String],
    userConfig: () => UserConfiguration,
) extends BuildTool
    with BuildServerProvider {

  lazy val runScalaCliCommand: Option[Seq[String]] =
    ScalaCli.localScalaCli(userConfig())

  override def generateBspConfig(
      workspace: AbsolutePath,
      systemProcess: List[String] => Future[BspConfigGenerationStatus],
  ): Future[BspConfigGenerationStatus] =
    createBspFileArgs(workspace).map(systemProcess).getOrElse {
      // fallback to creating `.bsp/scala-cli.json` that starts JVM launcher
      val bspConfig = workspace.resolve(".bsp").resolve("scala-cli.json")
      bspConfig.writeText(ScalaCli.scalaCliBspJsonContent())
      Future.successful(Generated)
    }

  def createBspConfigIfNone(
      workspace: AbsolutePath,
      systemProcess: List[String] => Future[BspConfigGenerationStatus],
  ): Future[BspConfigGenerationStatus] = {
    if (ScalaCliBuildTool.pathsToScalaCliBsp(workspace).exists(_.isFile))
      Future.successful(Generated)
    else generateBspConfig(workspace, systemProcess)
  }

  override def createBspFileArgs(
      workspace: AbsolutePath
  ): Option[List[String]] =
    runScalaCliCommand.map(
      _.toList ++ List(
        "setup-ide",
        workspace.toString(),
      )
    )

  override def bloopInstall(
      workspace: AbsolutePath,
      systemProcess: List[String] => Future[WorkspaceLoadedStatus],
  ): Future[WorkspaceLoadedStatus] =
    Future.successful(WorkspaceLoadedStatus.Dismissed)

  override def digest(workspace: AbsolutePath): Option[String] =
    ScalaCliDigest.current(workspace)

  override def minimumVersion: String = ScalaCli.minVersion

  override def version: String = workspaceVersion.getOrElse(recommendedVersion)

  override def recommendedVersion: String = BuildInfo.scalaCliVersion

  override def executableName: String = ScalaCliBuildTool.name

  override def isBloopDefaultBsp = false

}

object ScalaCliBuildTool {
  def name = "scala-cli"
  def pathsToScalaCliBsp(root: AbsolutePath): List[AbsolutePath] = List(
    root.resolve(".bsp").resolve("scala-cli.json"),
    root.resolve(".bsp").resolve("scala.json"),
  )

  def apply(
      root: AbsolutePath,
      userConfig: () => UserConfiguration,
  ): ScalaCliBuildTool = {
    val workspaceFolderVersions =
      for {
        path <- pathsToScalaCliBsp(root)
        text <- path.readTextOpt
        json = ujson.read(text)
        version <- json("version").strOpt
      } yield version
    new ScalaCliBuildTool(workspaceFolderVersions.headOption, userConfig)
  }
}

object ScalaCliDigest extends Digestable {
  override protected def digestWorkspace(
      workspace: AbsolutePath,
      digest: MessageDigest,
  ): Boolean = {
    ScalaCliBuildTool
      .pathsToScalaCliBsp(workspace)
      .foldRight(true)((path, acc) =>
        acc && Digest.digestFileBytes(path, digest)
      )
  }
}
