package co.blocke.scalajack.flexjson

object BijectiveFunction {

  def apply[A, B](apply: A ⇒ B, unapply: B ⇒ A): BijectiveFunction[A, B] =
    BijectiveFunctionPair(apply, unapply)

}

trait BijectiveFunction[A, B] extends Function[A, B] {

  override def apply(a: A): B

  def unapply(b: B): A

  def inverse: BijectiveFunction[B, A] =
    InvertedBijectiveFunction(this)

  def compose[X](f: BijectiveFunction[X, A]): BijectiveFunction[X, B] =
    ComposedBijectiveFunction(f, this)

  def andThen[C](g: BijectiveFunction[B, C]): BijectiveFunction[A, C] =
    ComposedBijectiveFunction(this, g)

}

case class BijectiveFunctionPair[A, B](applyFn: A ⇒ B,
                                       unapplyFn: B ⇒ A) extends BijectiveFunction[A, B] {

  override def apply(a: A): B = applyFn(a)

  override def unapply(b: B): A = unapply(b)

}

case class InvertedBijectiveFunction[A, B](f: BijectiveFunction[A, B]) extends BijectiveFunction[B, A] {

  override def apply(b: B): A = f.unapply(b)

  override def unapply(a: A): B = f.apply(a)

  override def inverse: BijectiveFunction[A, B] = f

}

case class ComposedBijectiveFunction[A, B, C](f: BijectiveFunction[A, B],
                                              g: BijectiveFunction[B, C]) extends BijectiveFunction[A, C] {

  override def apply(a: A): C = g.apply(f.apply(a))

  override def unapply(c: C): A = f.unapply(g.unapply(c))

}
