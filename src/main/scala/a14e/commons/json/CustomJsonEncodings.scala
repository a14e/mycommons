package a14e.commons.json

import scala.language.implicitConversions

trait CustomJsonEncodings
  extends  EnumEncodings
    with TaggedEncodings

object CustomJsonEncodings extends CustomJsonEncodings

