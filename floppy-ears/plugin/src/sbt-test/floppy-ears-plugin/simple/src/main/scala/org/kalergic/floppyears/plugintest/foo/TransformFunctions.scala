package org.kalergic.floppyears.plugintest.foo

import org.kalergic.floppyears.plugintest.{SampleData, SampleInput}

object TransformFunctions {
  val sampleInputToSampleData: SampleInput => SampleData = si =>
    SampleData(si.id)
  val sampleDataIncrement: SampleData => SampleData = sd => sd.copy(sd.id + 1)
}
