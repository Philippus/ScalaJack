package co.blocke.scalajack
package test.v4

import org.scalatest.{ FunSpec, GivenWhenThen, BeforeAndAfterAll }
import org.scalatest.Matchers._
import scala.language.postfixOps
import scala.util.Try
import org.joda.time.{DateTime,DateTimeZone}
import org.joda.time.format.DateTimeFormat

case class NCStuff(name:String,age:Int)

case class A(
    a:Map[List[String],Int]
    )

case class B(
    b:Map[Map[Boolean,String],Int]
    )

case class C(
    c:Map[NCStuff,Int]
    )

case class D(
    d:Map[(Int,String),Int]
    )

case class E(
    e:Map[Int,String]
    )


class NonCanonicalSpec extends FunSpec {
	val sjJS     = ScalaJack()
	val vc_nc_v  = VisitorContext().copy(isCanonical = false, isValidating = true) 
	val vc_c_v   = VisitorContext().copy(isValidating = true) 
	val vc_nc_nv = VisitorContext().copy(isCanonical = false) 
	// No vc = c_nv canonical (c) and non-validating (nv)

	object JSMaster {
		val a = """{"a":{["a","b","c"]:15}}"""
		val b = """{"b":{{false:"wow"}:15}}"""
		val c = """{"c":{{"name":"Greg","age":50}:15}}"""
		val d = """{"d":{[4,"nine"]:15}}"""
		val e = """{"e":{19:"boom"}}"""
	}

	object ScalaMaster {
		val a = A(Map(List("a", "b", "c") -> 15))
		val b = B(Map(Map(false -> "wow") -> 15))
		val c = C(Map(NCStuff("Greg",50) -> 15))
		val d = D(Map((4,"nine") -> 15))
		val e = E(Map(19 -> "boom"))
	}

	describe("============================\n| -- NonCanonical Tests -- |\n============================") {
		it("Render Tests - NC - V") {
			sjJS.render( ScalaMaster.a, vc_nc_v ) should be( JSMaster.a )
			sjJS.render( ScalaMaster.b, vc_nc_v ) should be( JSMaster.b )
			sjJS.render( ScalaMaster.c, vc_nc_v ) should be( JSMaster.c )
			sjJS.render( ScalaMaster.d, vc_nc_v ) should be( JSMaster.d )
			sjJS.render( ScalaMaster.e, vc_nc_v ) should be( JSMaster.e )
		}
		it( "Read Tests - NC - V" ) {
			sjJS.read[A]( JSMaster.a, vc_nc_v ) should be( ScalaMaster.a )
			sjJS.read[B]( JSMaster.b, vc_nc_v ) should be( ScalaMaster.b )
			sjJS.read[C]( JSMaster.c, vc_nc_v ) should be( ScalaMaster.c )
			sjJS.read[D]( JSMaster.d, vc_nc_v ) should be( ScalaMaster.d )
			sjJS.read[E]( JSMaster.e, vc_nc_v ) should be( ScalaMaster.e )
		}
		it("Render Tests - C - V (should be broken)") {
			an [co.blocke.scalajack.RenderException] should be thrownBy sjJS.render( ScalaMaster.a, vc_c_v )
			an [co.blocke.scalajack.RenderException] should be thrownBy sjJS.render( ScalaMaster.b, vc_c_v )
			an [co.blocke.scalajack.RenderException] should be thrownBy sjJS.render( ScalaMaster.c, vc_c_v )
			an [co.blocke.scalajack.RenderException] should be thrownBy sjJS.render( ScalaMaster.d, vc_c_v )
			an [co.blocke.scalajack.RenderException] should be thrownBy sjJS.render( ScalaMaster.e, vc_c_v )
		}
		it( "Read Tests - C - V (should be broken)" ) {
			an [co.blocke.scalajack.json.JsonParseException] should be thrownBy sjJS.read[A]( JSMaster.a, vc_c_v )
			an [co.blocke.scalajack.json.JsonParseException] should be thrownBy sjJS.read[B]( JSMaster.b, vc_c_v )
			an [co.blocke.scalajack.json.JsonParseException] should be thrownBy sjJS.read[C]( JSMaster.c, vc_c_v )
			an [co.blocke.scalajack.json.JsonParseException] should be thrownBy sjJS.read[D]( JSMaster.d, vc_c_v )
			an [co.blocke.scalajack.json.JsonParseException] should be thrownBy sjJS.read[E]( JSMaster.e, vc_c_v )
		}
		it("Render Tests - C - NV (should be broken) = no VC") {
			an [co.blocke.scalajack.RenderException] should be thrownBy sjJS.render( ScalaMaster.a )
			an [co.blocke.scalajack.RenderException] should be thrownBy sjJS.render( ScalaMaster.b )
			an [co.blocke.scalajack.RenderException] should be thrownBy sjJS.render( ScalaMaster.c )
			an [co.blocke.scalajack.RenderException] should be thrownBy sjJS.render( ScalaMaster.d )
			an [co.blocke.scalajack.RenderException] should be thrownBy sjJS.render( ScalaMaster.e )
		}
		it( "Read Tests - C - NV (should be broken) = no VC" ) {
			an [co.blocke.scalajack.json.JsonParseException] should be thrownBy sjJS.read[A]( JSMaster.a )
			an [co.blocke.scalajack.json.JsonParseException] should be thrownBy sjJS.read[B]( JSMaster.b )
			an [co.blocke.scalajack.json.JsonParseException] should be thrownBy sjJS.read[C]( JSMaster.c )
			an [co.blocke.scalajack.json.JsonParseException] should be thrownBy sjJS.read[D]( JSMaster.d )
			an [co.blocke.scalajack.json.JsonParseException] should be thrownBy sjJS.read[E]( JSMaster.e )
		}
		it("Render Tests - NC - NV") {
			sjJS.render( ScalaMaster.a, vc_nc_nv ) should be( JSMaster.a )
			sjJS.render( ScalaMaster.b, vc_nc_nv ) should be( JSMaster.b )
			sjJS.render( ScalaMaster.c, vc_nc_nv ) should be( JSMaster.c )
			sjJS.render( ScalaMaster.d, vc_nc_nv ) should be( JSMaster.d )
			sjJS.render( ScalaMaster.e, vc_nc_nv ) should be( JSMaster.e )
		}
		it( "Read Tests - NC - NV" ) {
			sjJS.read[A]( JSMaster.a, vc_nc_nv ) should be( ScalaMaster.a )
			sjJS.read[B]( JSMaster.b, vc_nc_nv ) should be( ScalaMaster.b )
			sjJS.read[C]( JSMaster.c, vc_nc_nv ) should be( ScalaMaster.c )
			sjJS.read[D]( JSMaster.d, vc_nc_nv ) should be( ScalaMaster.d )
			sjJS.read[E]( JSMaster.e, vc_nc_nv ) should be( ScalaMaster.e )
		}
	}
}
