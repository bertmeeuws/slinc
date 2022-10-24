package fr.hammons.slinc

import scala.concurrent.ExecutionContext
import scala.quoted.staging.Compiler
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import dotty.tools.dotc.config.Platform
import java.util.concurrent.ThreadFactory

trait Slinc:
  protected def jitManager: JitManager

  protected def layoutPlatformSpecific: LayoutI.PlatformSpecific
  protected def scopePlatformSpecific: ScopeI.PlatformSpecific
  protected def transitionsPlatformSpecific: TransitionsI.PlatformSpecific
  protected def libraryIPlatformSpecific: LibraryI.PlatformSpecific

  private val useJit = Option(System.getProperty("sffi-jit"))
    .flatMap(_.nn.toBooleanOption)
    .getOrElse(true)
  protected val layoutI = LayoutI(layoutPlatformSpecific)
  protected val transitionsI = TransitionsI(transitionsPlatformSpecific)
  protected val structI = StructI(layoutI, transitionsI, jitManager)
  protected val typesI = TypesI.platformTypes(layoutI)
  protected val scopeI = ScopeI(scopePlatformSpecific)
  protected val libraryI = LibraryI(libraryIPlatformSpecific)
  val receiveI = ReceiveI(libraryIPlatformSpecific, layoutI)
  export layoutI.{*, given}
  export typesI.{*, given}
  export libraryI.*
  export transitionsI.given
  export structI.Struct
  export scopeI.given
  export ContextProof.given
  export receiveI.given

  extension (l: Long) def toBytes = Bytes(l)
  
