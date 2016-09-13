package co.blocke.scalajack.flexjson.typeadapter

import co.blocke.scalajack.flexjson.FlexJsonFlavor.MemberName
import co.blocke.scalajack.flexjson.{ Context, ForwardingWriter, Reader, Reflection, TokenType, TypeAdapter, TypeAdapterFactory, Writer }

import scala.reflect.runtime.currentMirror
import scala.reflect.runtime.universe.{ ClassSymbol, Type, appliedType, typeOf }
import scala.collection.mutable.{ Map => MMap }

case class PolymorphicTypeAdapterFactory(hintFieldName: String) extends TypeAdapterFactory.FromClassSymbol {

  override def typeAdapter(tpe: Type, classSymbol: ClassSymbol, context: Context): Option[TypeAdapter[_]] =
    if (classSymbol.isTrait) {
      Some(PolymorphicTypeAdapter(hintFieldName, context.typeAdapterOf[Type], context.typeAdapterOf[MemberName], context, tpe))
    } else {
      None
    }

}

class PolymorphicWriter(
    override val delegate: Writer,
    typeFieldName:         String,
    tpe:                   Type,
    typeTypeAdapter:       TypeAdapter[Type],
    memberNameTypeAdapter: TypeAdapter[MemberName]
) extends ForwardingWriter {

  var depth = 0

  override def beginObject(): Unit = {
    depth += 1
    super.beginObject()

    if (depth == 1) {
      memberNameTypeAdapter.write(typeFieldName, this)
      typeTypeAdapter.write(tpe, this)
    }
  }

  override def endObject(): Unit = {
    depth -= 1
    super.endObject()
  }

}

object PolymorphicTypeAdapter {
  private val resolved: MMap[(Type, List[Type]), List[Type]] = MMap.empty[(Type, List[Type]), List[Type]]
}

case class PolymorphicTypeAdapter[T](
    typeMemberName:        MemberName,
    typeTypeAdapter:       TypeAdapter[Type],
    memberNameTypeAdapter: TypeAdapter[MemberName],
    context:               Context,
    polymorphicType:       Type
) extends TypeAdapter[T] {

  import scala.collection.mutable

  val populatedConcreteTypeCache = new mutable.WeakHashMap[Type, Type]

  def populateConcreteType(concreteType: Type): Type =
    populatedConcreteTypeCache.getOrElseUpdate(concreteType, Reflection.populateChildTypeArgs(polymorphicType, concreteType))

  override def read(reader: Reader): T = {
    if (reader.peek == TokenType.Null) {
      reader.readNull().asInstanceOf[T]
    } else {
      val originalPosition = reader.position

      reader.beginObject()

      var optionalConcreteType: Option[Type] = None

      while (optionalConcreteType.isEmpty && reader.hasMoreMembers) {
        val memberName = memberNameTypeAdapter.read(reader)

        if (memberName == typeMemberName) {
          val concreteType = typeTypeAdapter.read(reader)
          optionalConcreteType = Some(concreteType)
        } else {
          reader.skipValue()
        }
      }

      val concreteType = optionalConcreteType.getOrElse(throw new Exception(s"""Could not find type field named "$typeMemberName" """))
      val populatedConcreteType = populateConcreteType(concreteType)
      val concreteTypeAdapter = context.typeAdapter(populatedConcreteType)

      reader.position = originalPosition

      concreteTypeAdapter.read(reader).asInstanceOf[T]
    }
  }

  override def write(value: T, writer: Writer): Unit = {
    // TODO figure out a better way to infer the type (perhaps infer the type arguments?)
    val concreteType = currentMirror.classSymbol(value.getClass).toType
    val populatedConcreteType = populateConcreteType(concreteType)
    val valueTypeAdapter = context.typeAdapter(populatedConcreteType).asInstanceOf[TypeAdapter[T]]

    val polymorphicWriter = new PolymorphicWriter(writer, typeMemberName, populatedConcreteType, typeTypeAdapter, memberNameTypeAdapter)
    valueTypeAdapter.write(value, polymorphicWriter)
  }

}
