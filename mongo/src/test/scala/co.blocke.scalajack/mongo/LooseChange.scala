package co.blocke.scalajack
package mongo

import model._

import org.scalatest.FunSpec
import org.scalatest.Matchers._
import org.mongodb.scala.bson._

class LooseChange extends FunSpec {
  val sjM = ScalaJack(MongoFlavor())

  object MongoMaster {
    val a = BsonDocument("name" -> "Fred", "stuff" -> BsonDocument("a" -> 1, "b" -> true))
    val b = BsonDocument("name" -> "Fred", "stuff" -> BsonDocument("a" -> 1, "b" -> BsonArray(4, 5, 6)))
    val c = BsonDocument("name" -> "Fred", "stuff" -> BsonDocument("a" -> 1, "b" -> BsonArray(
      BsonDocument("x" -> "Fido", "y" -> false),
      BsonDocument("x" -> "Cat", "y" -> true)
    )))
    val e = BsonDocument("name" -> "Fred", "stuff" -> BsonDocument("a" -> 1, "b" -> BsonArray("foo", BsonNull(), "bar")))
    val f = BsonDocument("name" -> "Fred", "stuff" -> BsonDocument("a" -> 1, "b" -> 1.23))
    val g = BsonDocument("name" -> "Fred", "stuff" -> BsonDocument("a" -> 1, "b" -> 25L))
  }

  object ScalaMaster {
    val a = Something("Fred", Map("a" -> 1, "b" -> true))
    val b = Something("Fred", Map("a" -> 1, "b" -> List(4, 5, 6)))
    val c = Something("Fred", Map("a" -> 1, "b" -> List(Map("x" -> "Fido", "y" -> false), Map("x" -> "Cat", "y" -> true))))
    val e = Something("Fred", Map("a" -> 1, "b" -> List("foo", null, "bar")))
    val f = Something("Fred", Map("a" -> 1, "b" -> 1.23))
    val g = Something("Fred", Map("a" -> 1, "b" -> 25L))
  }

  describe("----------------------------\n:  Loose Change (MongoDB) :\n----------------------------") {
    it("Handles null value") {
      val dbo = BsonDocument("name" -> "Fred", "stuff" -> BsonDocument("a" -> 1, "b" -> 15))
      sjM.read[Something](dbo) should be(Something("Fred", Map("a" -> 1, "b" -> 15)))
    }
    it("Should blow up for unsupported BSON type") {
      val dbo = BsonDocument("name" -> "Fred", "stuff" -> BsonDocument("a" -> BsonJavaScript("code here")))
      the[SJError] thrownBy sjM.read[Something](dbo) should have message """BSON type org.bson.BsonJavaScript is not currently supported in ScalaJack."""
    }
    it("Field name remapping must work") {
      val mfp = MapFactor("wonder", 25L, 3, "hungry")
      val dbo = sjM.render(mfp)
      dbo.asDocument.toJson should be("""{"foo_bar": "wonder", "a_b": {"$numberLong": "25"}, "count": 3, "big_mac": "hungry"}""")
      sjM.read[MapFactor](dbo) should be(mfp)
    }
    it("Field name remapping on dbkey must work") {
      // val mfp = MapFactorId2("wonder", 25L, 1, 3)
      val mfp = MapFactorId("wonder", 25L, 3, "hungry")
      val dbo = sjM.render(mfp)
      dbo.asDocument.toJson should be("""{"_id": "wonder", "a_b": {"$numberLong": "25"}, "count": 3, "big_mac": "hungry"}""")
      sjM.read[MapFactorId](dbo) should be(mfp)
    }
    it("Field name remapping on dbkey with multi-part keys must work") {
      val mfp = MapFactorId2("wonder", 25L, 1, 3, "hungry")
      val dbo = sjM.render(mfp)
      dbo.asDocument.toJson should be("""{"_id": {"foo_bar": "wonder", "a_b": {"$numberLong": "25"}, "hey": 1}, "count": 3, "big_mac": "hungry"}""")
      sjM.read[MapFactorId2](dbo) should be(mfp)
    }
  }
}