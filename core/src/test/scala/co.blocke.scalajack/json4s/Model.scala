package co.blocke.scalajack
package json4s

import java.util.UUID

// === Scala
case class SampleBigDecimal(bd1: BigDecimal, bd2: BigDecimal, bd3: BigDecimal, bd4: BigDecimal, bd5: BigDecimal, bd6: BigDecimal)
case class SampleBigInt(bi1: BigInt, bi2: BigInt, bi3: BigInt, bi4: BigInt)
case class SampleBinary(b1: Array[Byte], b2: Array[Byte])
case class SampleBoolean(bool1: Boolean, bool2: Boolean)
case class SampleByte(b1: Byte, b2: Byte, b3: Byte, b4: Byte)
case class SampleChar(c1: Char, c2: Char, c3: Char)
case class SampleDouble(d1: Double, d2: Double, d3: Double, d4: Double)

object Size extends Enumeration {
  val Small, Medium, Large = Value
}
case class SampleEnum(e1: Size.Value, e2: Size.Value, e3: Size.Value, e4: Size.Value, e5: Size.Value)

case class SampleFloat(f1: Float, f2: Float, f3: Float, f4: Float)
case class SampleInt(i1: Int, i2: Int, i3: Int, i4: Int)
case class SampleLong(l1: Long, l2: Long, l3: Long, l4: Long)
case class SampleShort(s1: Short, s2: Short, s3: Short, s4: Short)
case class SampleString(s1: String, s2: String, s3: String)

case class SampleUUID(u1: UUID, u2: UUID)

case class Player(name: String, age: Int)
case class PlayerCapture(name: String, age: Int) extends SJCapture

case class OptionBigInt(o: Option[BigInt])
case class OptionClass(name: String, age: Option[Int])
case class OptionTuple(foo: Int, t: (Boolean, Option[String], Int))
trait Person { val name: String }
case class SomeClass(name: String, age: Int) extends Person
trait Thing[A, B] { val a: A; val b: B }
case class AThing[Y, X](a: X, b: Y) extends Thing[X, Y]

case class WrappedMaps(a: Map[Byte, Int], b: Map[Int, Int], c: Map[Long, Int], d: Map[Double, Int], e: Map[Float, Int], f: Map[Short, Int], g: Map[BigInt, Int], h: Map[BigDecimal, Int], i: Map[Boolean, Int], j: Map[Char, Int])

trait Address { val postalCode: String }
case class USAddress(street: String, city: String, state: String, postalCode: String) extends Address
case class DefaultAddress(postalCode: String) extends Address
trait Demographic { val address: Address }
case class USDemographic(age: Int, address: Address) extends Demographic
