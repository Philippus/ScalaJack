package co.blocke.scalajack
package test

import flexjson._
import flexjson.typeadapter.BasicTypeAdapter

import org.scalatest.{ FunSpec, GivenWhenThen, BeforeAndAfterAll }
import org.scalatest.Matchers._

object Special {
  type Phone = String
  type CreditCard = String
}
import Special._

case class Contact(
  name:  String,
  phone: Phone
)
case class Payment(
  name: String,
  cc:   CardWrapper
)

object PhoneAdapter extends BasicTypeAdapter[Phone] {
  override def read(reader: Reader): Phone = {
    reader.peek match {
      case TokenType.String ⇒
        val raw = Unicode.unescape_perl_string(reader.readString())
        "%s-%s-%s".format(raw.substring(0, 3), raw.substring(3, 6), raw.substring(6)).asInstanceOf[Phone]
      case TokenType.Null ⇒
        reader.readNull()
    }
  }

  override def write(value: Phone, writer: Writer): Unit =
    if (value == null) {
      writer.writeNull()
    } else {
      writer.writeString(value.replaceAll("-", ""))
    }
}

object CreditCardAdapter extends BasicTypeAdapter[CreditCard] {
  override def read(reader: Reader): CreditCard = {
    reader.peek match {
      case TokenType.String ⇒
        val raw = Unicode.unescape_perl_string(reader.readString())
        "%s %s %s %s".format(raw.substring(0, 4), raw.substring(4, 8), raw.substring(8, 12), raw.substring(12)).asInstanceOf[CreditCard]
      case TokenType.Null ⇒
        reader.readNull()
    }
  }

  override def write(value: CreditCard, writer: Writer): Unit =
    if (value == null) {
      writer.writeNull()
    } else {
      writer.writeString(value.replaceAll(" ", ""))
    }
}
class CardWrapper(val underlying: CreditCard) extends AnyVal

class Bar extends FunSpec with GivenWhenThen with BeforeAndAfterAll {
  val sj = ScalaJack()
  describe("-- More Cases --") {
    it("Case 1 - Primitive adapter") {
      val vc = VisitorContext().withAdapter(PhoneAdapter)
      val c = Contact("Greg", "123-456-7890")
      val js = sj.render(c, vc)
      js should equal("""{"name":"Greg","phone":"1234567890"}""")
      val obj = sj.read[Contact](js, vc)
      obj should equal(c)
    }
    it("Case 2 - Value class") {
      val vc = VisitorContext().withAdapter(CreditCardAdapter)
      val c = Payment("Greg", new CardWrapper("1234 5678 9012 3456"))
      val js = sj.render(c, vc)
      js should equal("""{"name":"Greg","cc":"1234567890123456"}""")
      val obj = sj.read[Payment](js, vc)
      println(obj.cc.underlying)
      obj should equal(c)
      obj.cc.underlying should equal("1234 5678 9012 3456")
      // Just create a BasicTypeAdapter then define a value class wrapping that basic type and see what happens...
    }
  }
}