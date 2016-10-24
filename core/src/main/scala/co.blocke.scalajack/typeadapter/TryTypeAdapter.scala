package co.blocke.scalajack
package typeadapter

import scala.reflect.runtime.universe.{ TypeTag, typeOf }
import scala.util.{ Failure, Success, Try }

object TryTypeAdapter extends TypeAdapterFactory {

  override def typeAdapterOf[T](context: Context, next: TypeAdapterFactory)(implicit tt: TypeTag[T]): TypeAdapter[T] =
    if (tt.tpe <:< typeOf[Try[_]]) {
      val valueType = tt.tpe.typeArgs.head
      val valueTypeAdapter = context.typeAdapter(valueType)

      TryTypeAdapter(valueTypeAdapter).asInstanceOf[TypeAdapter[T]]
    } else {
      next.typeAdapterOf[T](context)
    }

}

case class TryTypeAdapter[T](valueTypeAdapter: TypeAdapter[T]) extends TypeAdapter[Try[T]] {

  override def read(reader: Reader): Try[T] = {
    val originalPosition = reader.position

    val attempt = Try { valueTypeAdapter.read(reader) }

    attempt match {
      case self @ Success(_) ⇒
        self

      case Failure(cause) ⇒
        Failure(new UnreadableException(reader.captureValue(), cause))
    }
  }

  override def write(value: Try[T], writer: Writer): Unit =
    value match {
      case Success(v) ⇒
        valueTypeAdapter.write(v, writer)

      case Failure(e: UnreadableException) ⇒
        e.write(writer)

      case Failure(e) ⇒
        throw e
    }

}
