import os.Path
import $file.benchmark, benchmark.BenchmarksModule
import $file.publishable, publishable.PublishableModule
import $file.facadeGenerator, facadeGenerator.FacadeGenerationModule
import mill._, scalalib._, scalafmt._
import $ivy.`com.lihaoyi::mill-contrib-buildinfo:`
import mill.contrib.buildinfo.BuildInfo
import com.github.lolgab.mill.mima._

import $ivy.`de.tototec::de.tobiasroeser.mill.jacoco_mill0.10:0.0.2`
import de.tobiasroeser.mill.jacoco.JacocoTestModule

object v {
  val munit = "1.0.0-M6"
  val jmh = "1.33"
  val jnr = "2.2.3"
  val jna = "5.9.0"
  val scoverage = "1.4.0"
}

trait BaseModule extends ScalaModule with ScalafmtModule {
  def scalaVersion = "3.2.0"

  val munitVersion = "1.0.0-M6"
  val jmhV = "1.33"
  val jnr = "2.2.3"
  val jna = "5.9.0"

  def ivyDeps = Agg(
    ivy"org.scala-lang::scala3-staging:${scalaVersion()}"
  )

  def scalacOptions = Seq(
    "-deprecation",
    "-Wunused:all",
    "-unchecked",
    "-Xcheck-macros",
    "-Xprint-suspension",
    "-Xsemanticdb",
    "-Yexplicit-nulls",
    "-Ysafe-init",
    "-source:future",
    "-Ykind-projector"
  )
}
object core extends BaseModule with PublishableModule with FacadeGenerationModule with BenchmarksModule {

  def pomSettings = pomTemplate("slinc-core")

  def specializationArity = 4

  object test extends Tests with TestModule.Munit with JacocoTestModule {
    def ivyDeps = Agg(ivy"org.scalameta::munit:$munitVersion")
  }

  object benchmarks extends BaseModule  {
    def moduleDeps = Seq(core)
    override def scalaVersion = core.scalaVersion()
    override def scalacOptions = core.scalacOptions

    object test extends BenchmarkSources {
      def jmhVersion = jmhV 
      def forkArgs = super.forkArgs() ++ Seq(
        "--add-modules=jdk.incubator.foreign",
        "--enable-native-access=ALL-UNNAMED"
      )

    }
  }

}

object j17 extends BaseModule with PublishableModule with BenchmarksModule {
  def moduleDeps = Seq(core)
  def pomSettings = pomTemplate("slinc-java-17")

  def javacOptions = super.javacOptions() ++ Seq("--add-modules=jdk.incubator.foreign")

  object test extends Tests with TestModule.Munit with JacocoTestModule {
    def ivyDeps = Agg(ivy"org.scalameta::munit:$munitVersion")
    def moduleDeps = super.moduleDeps ++ Seq(core.test)
    def forkArgs = super.forkArgs() ++ Seq(
      "--add-modules=jdk.incubator.foreign",
      "--enable-native-access=ALL-UNNAMED"
    )
  }

  //todo: remove this nasty hack needed for jacoco coverage reports
  object benchmarks extends BaseModule{
    def moduleDeps = Seq(j17)
    override def scalaVersion = j17.scalaVersion
    override def scalacOptions = j17.scalacOptions
    object test extends Benchmarks {
      def moduleDeps = Seq(j17.benchmarks, core.benchmarks.test)
      def jmhVersion = jmhV
      def forkArgs = super.forkArgs() ++ Seq(
        "--add-modules=jdk.incubator.foreign",
        "--enable-native-access=ALL-UNNAMED"
      )
    }
  }

}