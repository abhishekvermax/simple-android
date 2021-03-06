package org.simple.clinic.bloodsugar.entry

import org.simple.clinic.bloodsugar.BloodSugarMeasurementType
import org.simple.clinic.bloodsugar.Fasting
import org.simple.clinic.bloodsugar.PostPrandial
import org.simple.clinic.bloodsugar.Random
import org.simple.clinic.mobius.ViewRenderer
import org.simple.clinic.util.ValueChangedCallback

class BloodSugarEntryUiRenderer(
    private val ui: BloodSugarEntryUi
) : ViewRenderer<BloodSugarEntryModel> {
  private val openAsValueChangedCallback = ValueChangedCallback<OpenAs>()

  override fun render(model: BloodSugarEntryModel) {
    openAsValueChangedCallback.pass(model.openAs) { setupUi(it) }
  }

  private fun setupUi(openAs: OpenAs) {
    when (openAs) {
      is New -> showEnterNewBloodSugarTitle(openAs.measurementType)
    }
  }

  private fun showEnterNewBloodSugarTitle(measurementType: BloodSugarMeasurementType) {
    with(ui) {
      when (measurementType) {
        is Random -> showRandomBloodSugarTitle()
        is PostPrandial -> showPostPrandialBloodSugarTitle()
        is Fasting -> showFastingBloodSugarTitle()
      }
    }
  }
}
