package co.blocke.scalajack.flexjson

import co.blocke.scalajack.flexjson.typeadapter.javaprimitives.{ JavaBooleanTypeAdapter, JavaByteTypeAdapter, JavaCharacterTypeAdapter, JavaDoubleTypeAdapter, JavaFloatTypeAdapter, JavaIntegerTypeAdapter, JavaLongTypeAdapter, JavaShortTypeAdapter }
import co.blocke.scalajack.flexjson.typeadapter.joda.JodaDateTimeTypeAdapter
import co.blocke.scalajack.flexjson.typeadapter.{ AnyTypeAdapter, BigDecimalTypeAdapter, BooleanTypeAdapter, ByteTypeAdapter, CaseClassTypeAdapter, CharTypeAdapter, DerivedValueClassAdapter, DerivedValueClassCompanionTypeAdapter, DoubleTypeAdapter, EnumerationTypeAdapter, FloatTypeAdapter, IntTypeAdapter, ListTypeAdapter, LongTypeAdapter, MapTypeAdapter, OptionTypeAdapter, SetTypeAdapter, ShortTypeAdapter, StringTypeAdapter, TryTypeAdapter, TupleTypeAdapter, TypeTypeAdapter, UUIDTypeAdapter, _ }

import scala.reflect.runtime.universe.{ Type, TypeTag, typeOf }

object Context {

  val StandardContext = Context()
    .withFactory(AnyTypeAdapter)
    .withFactory(TypeTypeAdapter)
    .withFactory(ListTypeAdapter)
    .withFactory(SetTypeAdapter)
    .withFactory(MapTypeAdapter)
    .withFactory(TupleTypeAdapter)
    .withFactory(CaseClassTypeAdapter)
    .withFactory(OptionTypeAdapter)
    .withFactory(TryTypeAdapter)
    .withFactory(BooleanTypeAdapter)
    .withFactory(CharTypeAdapter)
    .withFactory(ByteTypeAdapter)
    .withFactory(ShortTypeAdapter)
    .withFactory(IntTypeAdapter)
    .withFactory(LongTypeAdapter)
    .withFactory(FloatTypeAdapter)
    .withFactory(DoubleTypeAdapter)
    .withFactory(BigDecimalTypeAdapter)
    .withFactory(StringTypeAdapter)
    .withFactory(DerivedValueClassCompanionTypeAdapter)
    .withFactory(DerivedValueClassAdapter)
    .withFactory(EnumerationTypeAdapter)
    .withFactory(JavaBooleanTypeAdapter)
    .withFactory(JavaByteTypeAdapter)
    .withFactory(JavaCharacterTypeAdapter)
    .withFactory(JavaDoubleTypeAdapter)
    .withFactory(JavaFloatTypeAdapter)
    .withFactory(JavaIntegerTypeAdapter)
    .withFactory(JavaLongTypeAdapter)
    .withFactory(JavaShortTypeAdapter)
    .withFactory(UUIDTypeAdapter)
    .withFactory(JodaDateTimeTypeAdapter)
}

case class Context(factories: List[TypeAdapterFactory] = Nil) {

  var resolvedTypeAdapters = Map[Type, TypeAdapter[_]]()

  def withFactory(factory: TypeAdapterFactory): Context =
    copy(factories = factories :+ factory)

  def typeAdapter(tpe: Type, superParamTypes: List[Type] = List.empty[Type]): TypeAdapter[_] = {
    val tsym = tpe.typeSymbol.asType.typeParams
    val args = tpe.typeArgs

    // if (resolvedTypeAdapters.contains(tpe)) println("FOUND: " + tpe)

    resolvedTypeAdapters.getOrElse(tpe, {
      var optionalTypeAdapter: Option[TypeAdapter[_]] = None

      var remainingFactories = factories
      while (optionalTypeAdapter.isEmpty && remainingFactories.nonEmpty) {
        optionalTypeAdapter = remainingFactories.head.typeAdapter(tpe, this, superParamTypes)
        remainingFactories = remainingFactories.tail
      }

      val typeAdapter = optionalTypeAdapter.getOrElse(throw new IllegalArgumentException(s"Cannot find a type adapter for $tpe"))

      // println(">> Saved: " + tpe.typeSymbol.fullName + superParamTypes.map(_.typeSymbol.fullName).mkString("(", ",", ")"))

      // For types that do substitution, we need to make sure the tpe that has type substitution done is the one used as
      // the key, otherwise the general tpe (pre-substitution) will be used--that's bad.  It'll store tpe for Foo[A], so
      // Foo[String] and Foo[Boolean] are both found!  Clearly not what we want.  Post-substitution the specific versions are
      // used as the key.
      //
      // NOTE/TODO: We may need a similar thing done for other parameterized types, e.g. collections.  To clean up code smell
      // we may want to subtype TypeAdapter, i.e. ParameterizedTypeAdapter or something
      //

      typeAdapter match {
        case ta: CaseClassTypeAdapter[_] =>
          resolvedTypeAdapters += ta.caseClassType → ta
        case ta: PolymorphicTypeAdapter[_] =>
          resolvedTypeAdapters += ta.polyType → ta
        case _ =>
          resolvedTypeAdapters += tpe → typeAdapter
      }

      typeAdapter
    })
  }

  def typeAdapterOf[T](implicit valueTypeTag: TypeTag[T]): TypeAdapter[T] =
    typeAdapter(valueTypeTag.tpe, valueTypeTag.tpe.typeArgs).asInstanceOf[TypeAdapter[T]]

}
