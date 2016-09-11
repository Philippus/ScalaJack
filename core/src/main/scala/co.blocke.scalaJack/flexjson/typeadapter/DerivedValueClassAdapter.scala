package co.blocke.scalajack.flexjson.typeadapter

import java.lang.reflect.Method

import co.blocke.scalajack.flexjson.{ Context, Reader, Reflection, TypeAdapter, TypeAdapterFactory, Writer }

import scala.reflect.runtime.currentMirror
import scala.reflect.runtime.universe.{ ClassSymbol, MethodMirror, MethodSymbol, TermName, Type }

object DerivedValueClassAdapter extends TypeAdapterFactory.FromClassSymbol {

  override def typeAdapter(tpe: Type, classSymbol: ClassSymbol, context: Context, superParamTypes: List[Type]): Option[TypeAdapter[_]] =
    if (classSymbol.isDerivedValueClass) {
      val constructorSymbol = classSymbol.primaryConstructor.asMethod
      val constructorMirror = currentMirror.reflectClass(classSymbol).reflectConstructor(constructorSymbol)

      val parameter = constructorSymbol.paramLists.head.head
      val parameterName = parameter.name.encodedName.toString
      val accessorMethodSymbol = tpe.member(TermName(parameterName)).asMethod
      val accessorMethod = Reflection.methodToJava(accessorMethodSymbol)

      val valueType = parameter.infoIn(tpe).substituteTypes(tpe.typeConstructor.typeParams, tpe.typeArgs)
      val valueTypeAdapter = context.typeAdapter(valueType, valueType.typeArgs)

      Some(DerivedValueClassAdapter(constructorMirror, accessorMethodSymbol, accessorMethod, valueTypeAdapter))
    } else {
      None
    }

}

case class DerivedValueClassAdapter[DerivedValueClass, Value](
    constructorMirror:    MethodMirror,
    accessorMethodSymbol: MethodSymbol,
    accessorMethod:       Method,
    valueTypeAdapter:     TypeAdapter[Value]
) extends TypeAdapter[DerivedValueClass] {

  override def read(reader: Reader): DerivedValueClass = {
    val value = valueTypeAdapter.read(reader)
    constructorMirror.apply(value).asInstanceOf[DerivedValueClass]
  }

  override def write(value: DerivedValueClass, writer: Writer): Unit = {
    val wrappedValue = accessorMethod.invoke(value).asInstanceOf[Value]
    valueTypeAdapter.write(wrappedValue, writer)
  }

}
