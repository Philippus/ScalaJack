package co.blocke.scalajack.flexjson

import java.util.concurrent.ConcurrentHashMap

import co.blocke.scalajack.flexjson.typeadapter.javaprimitives.{ JavaBooleanTypeAdapter, JavaByteTypeAdapter, JavaCharacterTypeAdapter, JavaDoubleTypeAdapter, JavaFloatTypeAdapter, JavaIntegerTypeAdapter, JavaLongTypeAdapter, JavaNumberTypeAdapter, JavaShortTypeAdapter }
import co.blocke.scalajack.flexjson.typeadapter.joda.JodaDateTimeTypeAdapter
import co.blocke.scalajack.flexjson.typeadapter.{ AnyTypeAdapter, BigDecimalTypeAdapter, BooleanTypeAdapter, ByteTypeAdapter, CaseClassTypeAdapter, CharTypeAdapter, DerivedValueClassAdapter, DerivedValueClassCompanionTypeAdapter, DoubleTypeAdapter, EnumerationTypeAdapter, FloatTypeAdapter, IntTypeAdapter, ListTypeAdapter, LongTypeAdapter, MapTypeAdapter, OptionTypeAdapter, SetTypeAdapter, ShortTypeAdapter, StringTypeAdapter, TryTypeAdapter, TupleTypeAdapter, TypeParameterTypeAdapter, TypeTypeAdapter, UUIDTypeAdapter }

import scala.language.existentials
import scala.reflect.runtime.universe.{ Type, TypeTag }
import scala.util.{ Success, Try }

object Context {

  val StandardContext = Context()
    .withFactory(TypeParameterTypeAdapter)
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
    // FIXME    .withFactory(PolymorphicTypeAdapter)
    .withFactory(JavaNumberTypeAdapter)
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

  sealed trait Phase
  case object Uninitialized extends Phase
  case object Initializing extends Phase
  case class Initialized(typeAdapterAttempt: Try[TypeAdapter[_]]) extends Phase

  object TypeEntryFactory extends java.util.function.Function[Type, TypeEntry] {
    override def apply(tpe: Type): TypeEntry = new TypeEntry(tpe)
  }

  class TypeEntry(tpe: Type) {

    @volatile
    private var phase: Phase = Uninitialized

    def typeAdapter: TypeAdapter[_] = {
      val attempt =
        phase match {
          case Initialized(a) ⇒ a

          case Uninitialized | Initializing ⇒
            synchronized {
              phase match {
                case Uninitialized ⇒
                  phase = Initializing

                  val typeAdapterAttempt = Try {
                    var optionalTypeAdapter: Option[TypeAdapter[_]] = None

                    var remainingFactories = factories
                    while (optionalTypeAdapter.isEmpty && remainingFactories.nonEmpty) {
                      optionalTypeAdapter = remainingFactories.head.typeAdapter(tpe, Context.this)
                      remainingFactories = remainingFactories.tail
                    }

                    optionalTypeAdapter.getOrElse(throw new IllegalArgumentException(s"Cannot find a type adapter for $tpe"))
                  }

                  phase = Initialized(typeAdapterAttempt)

                  typeAdapterAttempt

                case Initializing ⇒
                  Success(LazyTypeAdapter(Context.this, tpe))

                case Initialized(a) ⇒
                  a
              }
            }
        }

      attempt.get
    }

  }

  private val typeEntries = new ConcurrentHashMap[Type, TypeEntry]

  def withFactory(factory: TypeAdapterFactory): Context =
    copy(factories = factories :+ factory)

  def typeAdapter(tpe: Type): TypeAdapter[_] =
    typeEntries.computeIfAbsent(tpe, TypeEntryFactory).typeAdapter

  def typeAdapterOf[T](implicit valueTypeTag: TypeTag[T]): TypeAdapter[T] =
    typeAdapter(valueTypeTag.tpe).asInstanceOf[TypeAdapter[T]]

}
