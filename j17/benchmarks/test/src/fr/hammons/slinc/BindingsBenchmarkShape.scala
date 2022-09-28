package fr.hammons.slinc

import org.openjdk.jmh.annotations.*,
Mode.{SampleTime, SingleShotTime, Throughput}
import java.util.concurrent.TimeUnit
import fr.hammons.slinc.Scope
import jdk.incubator.foreign.MemorySegment
import jdk.incubator.foreign.SegmentAllocator
import scala.util.Random

case class div_t(quot: Int, rem: Int)
trait BindingsBenchmarkShape(val s: Slinc):
  import scala.language.unsafeNulls
  import s.{given, *}

  object Cstd derives Library:
    def abs(i: Int): Int = Library.binding
    def div(numer: Int, denom: Int): div_t = Library.binding
    // todo: needs SizeT
    def qsort[A](
        array: Ptr[A],
        num: Long,
        size: Long,
        fn: Ptr[(Ptr[A], Ptr[A]) => A]
    ): Unit = Library.binding

  given Struct[div_t] = Struct.derived

  val lib = summon[Library[Cstd.type]]
  val absHandle = lib.handles(0)

  val base = Seq.fill(10000)(Random.nextInt)
  val baseArr = base.toArray
  
  val upcall: Ptr[(Ptr[Int], Ptr[Int]) => Int] = Scope.global {
    Ptr.upcall((a, b) =>
      val aVal = !a
      val bVal = !b
      if aVal < bVal then -1
      else if aVal == bVal then 0
      else 1
    )
  }

  @Benchmark
  def abs =
    Cstd.abs(6)

  @Benchmark
  def absUnboxed =
    MethodHandleFacade.callExact(lib.handles(0), 6)

  @Benchmark
  def div =
    Cstd.div(5, 2)

  @Benchmark
  def divSpecialized =
    Scope.temp(alloc ?=>
      summon[Receive[div_t]].from(
        Mem17(
          MethodHandleFacade
            .call2Int(lib.handles(1), alloc.base, 5, 2)
            .asInstanceOf[MemorySegment]
        ),
        0.toBytes
      )
    )

  @Benchmark
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  def qsort =
    Scope.confined {
      val sortingArr = Ptr.copy(baseArr)
      Cstd.qsort(
        sortingArr,
        10000,
        4,
        Ptr.upcall((a, b) =>
          val aVal = !a
          val bVal = !b
          if aVal < bVal then -1
          else if aVal == bVal then 0
          else 1
        )
      )
      sortingArr.asArray(10000)
    }

  @Benchmark
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  def scalasort =
    baseArr.sorted
