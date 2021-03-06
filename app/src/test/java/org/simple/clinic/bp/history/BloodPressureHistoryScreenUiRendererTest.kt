package org.simple.clinic.bp.history

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import org.junit.Test
import org.simple.clinic.patient.PatientMocker
import java.util.UUID

class BloodPressureHistoryScreenUiRendererTest {

  private val ui = mock<BloodPressureHistoryScreenUi>()
  private val renderer = BloodPressureHistoryScreenUiRenderer(ui)
  private val patientUuid = UUID.fromString("9dd563b5-99a5-4f43-b3ab-47c43ed5d62c")
  private val defaultModel = BloodPressureHistoryScreenModel.create(patientUuid)

  @Test
  fun `when blood pressure history is being loaded then do nothing`() {
    //when
    renderer.render(defaultModel)

    //then
    verifyZeroInteractions(ui)
  }

  @Test
  fun `when blood pressure history is loaded, then show it on the ui`() {
    //given
    val bloodPressure = PatientMocker.bp(
        UUID.fromString("4ca198c4-18f8-4a3d-8cc7-fc1f363241fa"),
        patientUuid
    )
    val bloodPressures = listOf(bloodPressure)

    //when
    renderer.render(defaultModel.historyLoaded(bloodPressures))

    //then
    verify(ui).showBloodPressureHistory(bloodPressures)
    verifyNoMoreInteractions(ui)
  }

  @Test
  fun `when patient is loaded, then show it on the ui`() {
    //given
    val patient = PatientMocker.patient(
        uuid = UUID.fromString("c80bce99-82bc-4d12-a85a-dcae373fece3")
    )

    //when
    renderer.render(defaultModel.patientLoaded(patient))

    //then
    verify(ui).showPatientInformation(patient)
    verifyNoMoreInteractions(ui)
  }

  @Test
  fun `when both patient and blood pressure history is loaded, then show it on the ui`() {
    //given
    val patient = PatientMocker.patient(
        uuid = UUID.fromString("bf8b7e71-9b99-4d7b-9dfa-55f3c5e74730")
    )
    val bloodPressure = PatientMocker.bp(
        UUID.fromString("4ca198c4-18f8-4a3d-8cc7-fc1f363241fa"),
        patientUuid
    )
    val bloodPressures = listOf(bloodPressure)

    //when
    renderer.render(defaultModel.historyLoaded(bloodPressures).patientLoaded(patient))

    //then
    verify(ui).showBloodPressureHistory(bloodPressures)
    verify(ui).showPatientInformation(patient)
    verifyNoMoreInteractions(ui)
  }
}
