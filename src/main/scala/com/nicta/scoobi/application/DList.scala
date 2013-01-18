package com.nicta.scoobi
package application

import core._
import impl.plan.DListImpl
import impl.plan.source._

/** This object provides a set of operations to create distributed lists. */
trait DLists {

  /** Create a distributed list with given elements. */
  def apply[A : WireFormat](elems: A*): DList[A] = SeqInput.fromSeq(Vector(elems:_*))

  /** Create a distributed list of Ints from a Range. */
  def apply(range: Range): DList[Int] = SeqInput.fromSeq(range)

  /** Create a distributed list from a data source. */
  def fromSource[K, V, A : WireFormat](source: DataSource[K, V, A]): DList[A] =
    DListImpl(source)

  /** Concatenate all distributed lists into a single distributed list. */
  def concat[A : WireFormat](xss: List[DList[A]]): DList[A] = xss match {
    case Nil      => sys.error("'concat' must take a non-empty list.")
    case x :: Nil => x
    case x :: xs  => x ++ (xs: _*)
  }

  /** Concatenate all distributed lists into a single distributed list. */
  def concat[A : WireFormat](xss: DList[A]*): DList[A] = concat(xss.toList)

  /** Create a distributed list containing values of a given function over a range of
    * integer values starting from 0. */
  def tabulate[A : WireFormat](n: Int)(f: Int => A): DList[A] =
    FunctionInput.fromFunction(n)(f)

  /** Create a DList with the same element repeated n times. */
  def fill[A : WireFormat : Manifest](n: Int)(a: =>A): DList[A] =
    apply(Seq.fill(n)(a):_*)

  /** Pimping from generic collection types (i.e. Seq) to a Distributed List */
  implicit def traversableToDList[A : WireFormat : Manifest](traversable: Traversable[A]) = new TraversableToDList[A](traversable)
  class TraversableToDList[A : WireFormat : Manifest](traversable: Traversable[A]) {
    def toDList: DList[A] = apply(traversable.toSeq:_*)
  }
}

object DLists extends DLists
object DList extends DLists