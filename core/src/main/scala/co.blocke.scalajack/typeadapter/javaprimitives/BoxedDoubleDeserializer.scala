package co.blocke.scalajack
package typeadapter
package javaprimitives

import co.blocke.scalajack.typeadapter.javaprimitives.BoxedDoubleDeserializer.BoxedDoubleType

import scala.reflect.runtime.universe.{ Type, typeOf }

object BoxedDoubleDeserializer {

  private val BoxedDoubleType: Type = typeOf[java.lang.Double]

}

class BoxedDoubleDeserializer(doubleDeserializer: Deserializer[Double]) extends Deserializer[java.lang.Double] {

  override def deserialize[J](path: Path, json: J)(implicit ops: JsonOps[J]): DeserializationResult[java.lang.Double] =
    json match {
      case JsonNull() =>
        DeserializationSuccess(TypeTagged(null, BoxedDoubleType))

      case _ =>
        doubleDeserializer.deserialize(path, json) map {
          case TypeTaggedDouble(doubleValue) => TypeTagged(java.lang.Double.valueOf(doubleValue), BoxedDoubleType)
          case TypeTagged(doubleValue)       => TypeTagged(java.lang.Double.valueOf(doubleValue), BoxedDoubleType)
        }
    }

}