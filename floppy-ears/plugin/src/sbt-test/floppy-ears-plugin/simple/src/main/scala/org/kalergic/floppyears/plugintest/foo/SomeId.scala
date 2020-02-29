// This is in a subpackage to make sure the code generator handles cross-package dependencies.
package org.kalergic.floppyears.plugintest.foo

case class SomeId(id: String)

object SomeId extends (String => SomeId)
