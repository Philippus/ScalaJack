package co.blocke.scalajack.flexjson.typeadapter

import java.lang.reflect.Method

import co.blocke.scalajack.ValueClassCustom
import co.blocke.scalajack.flexjson.{ Context, Reader, Reflection, TypeAdapter, TypeAdapterFactory, Writer }
import co.blocke.scalajack.json.JsonKind

import scala.reflect.runtime.currentMirror
import scala.reflect.runtime.universe.{ ClassSymbol, MethodMirror, MethodSymbol, TermName, Type, typeOf }

object DerivedValueClassCompanionTypeAdapter extends TypeAdapterFactory.FromClassSymbol {

  override def typeAdapter(tpe: Type, classSymbol: ClassSymbol, context: Context): Option[TypeAdapter[_]] = {
    if (tpe.companion <:< typeOf[ValueClassCustom]) {
      val companionMirror = currentMirror.reflectModule(classSymbol.companion.asModule)
      val companion = companionMirror.instance.asInstanceOf[ValueClassCustom]

      companion match {
        case valueClassCustom: ValueClassCustom ⇒
          val constructorSymbol = classSymbol.primaryConstructor.asMethod
          val constructorMirror = currentMirror.reflectClass(classSymbol).reflectConstructor(constructorSymbol)

          val parameter = constructorSymbol.paramLists.head.head
          val parameterName = parameter.name.encodedName.toString
          val accessorMethodSymbol = tpe.member(TermName(parameterName)).asMethod
          val accessorMethod = Reflection.methodToJava(accessorMethodSymbol)

          val anyTypeAdapter = context.typeAdapterOf[Any]

          Some(DerivedValueClassCompanionTypeAdapter(constructorMirror, accessorMethodSymbol, accessorMethod, valueClassCustom, anyTypeAdapter))

        case _ ⇒
          None
      }
    } else {
      None
    }
  }

}

case class DerivedValueClassCompanionTypeAdapter[DerivedValueClass, Value](
    constructorMirror:         MethodMirror,
    valueAccessorMethodSymbol: MethodSymbol,
    valueAccessorMethod:       Method,
    valueClassCustom:          ValueClassCustom,
    anyTypeAdapter:            TypeAdapter[Any]
) extends TypeAdapter[DerivedValueClass] {

  override def read(reader: Reader): DerivedValueClass = {
    val rawValue = anyTypeAdapter.read(reader)
    val wrappedValue = valueClassCustom.read((JsonKind(), rawValue))
    constructorMirror.apply(wrappedValue).asInstanceOf[DerivedValueClass]
  }

  override def write(value: DerivedValueClass, writer: Writer): Unit = {
    val wrappedValue = valueAccessorMethod.invoke(value)
    val rawJson = valueClassCustom.render((JsonKind(), wrappedValue)).toString.toCharArray
    writer.writeRawValue(rawJson, 0, rawJson.length)
  }

}
