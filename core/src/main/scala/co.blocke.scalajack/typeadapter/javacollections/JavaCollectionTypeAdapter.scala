package co.blocke.scalajack
package typeadapter
package javacollections

object JavaCollectionTypeAdapter extends TypeAdapterFactory.<:<.withOneTypeParam[java.util.Collection] {

  override def create[E, C <: java.util.Collection[E]](next: TypeAdapterFactory)(implicit context: Context, tt: TypeTag[C], ttCollection: TypeTag[java.util.Collection[E]], ttElement: TypeTag[E]): TypeAdapter[C] = {
    val collectionConstructor: java.lang.reflect.Constructor[C] = runtimeClassOf[C].getConstructor()

    def newEmptyCollection(): C = collectionConstructor.newInstance()

    val elementTypeAdapter = context.typeAdapterOf[E]

    new JavaCollectionTypeAdapter[E, C](
      deserializer = new JavaCollectionDeserializer[E, C](elementTypeAdapter.deserializer, (() => newEmptyCollection())),
      serializer   = new JavaCollectionSerializer[E, C](elementTypeAdapter.serializer))
  }

}

class JavaCollectionTypeAdapter[E, C <: java.util.Collection[E]](override val deserializer: Deserializer[C], override val serializer: Serializer[C]) extends TypeAdapter[C]