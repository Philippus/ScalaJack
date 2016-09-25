package co.blocke.scalajack
package typeadapter
package javaprimitives

object JavaCharacterTypeAdapter extends SimpleTypeAdapter[java.lang.Character] {

  override def read(reader: Reader): java.lang.Character =
    reader.peek match {
      case TokenType.Null ⇒
        reader.readNull()

      case TokenType.String ⇒
        reader.read(expected = TokenType.String)
        java.lang.Character.valueOf(reader.tokenText.head)

      case actual ⇒ {
        reader.read()
        throw new IllegalStateException(s"Expected value token of type String, not $actual when reading Character value.  (Is your value wrapped in quotes?)\n" + reader.showError())
      }
    }

  override def write(value: java.lang.Character, writer: Writer): Unit =
    if (value == null) {
      writer.writeNull()
    } else {
      writer.writeChar(value.charValue)
    }

}