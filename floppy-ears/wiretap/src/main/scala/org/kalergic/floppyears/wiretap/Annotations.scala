package org.kalergic.floppyears.wiretap

import scala.annotation.StaticAnnotation

object Annotations {

  private[Annotations] sealed trait WiretapAnnotation extends StaticAnnotation

  final class Wiretap(actionName: String, majorVersion: Int)
      extends WiretapAnnotation

  final class WiretapRequest[A](
      requestBody: Class[A],
      parseJson: Boolean = true
  ) extends WiretapAnnotation
  final class WiretapRequestTransform[A, B](
      transformedBody: Class[B],
      transform: A => B
  ) extends WiretapAnnotation

  final class WiretapResponse[A](responseBody: Class[A])
      extends WiretapAnnotation
  final class WiretapResponseTransform[A, B](
      transformedBody: Class[B],
      transform: A => B
  ) extends WiretapAnnotation

  final class WiretapIgnore extends WiretapAnnotation

  final class WiretapConvert[A](convert: String => A) extends WiretapAnnotation
}
