package org.simple.clinic.summary

import org.simple.clinic.facility.Facility
import org.simple.clinic.widgets.UiEvent
import org.threeten.bp.Instant
import java.util.UUID

sealed class PatientSummaryEvent : UiEvent

data class PatientSummaryProfileLoaded(val patientSummaryProfile: PatientSummaryProfile) : PatientSummaryEvent()

// TODO(vs): 2020-01-15 Consider whether these should be moved to the effect handler as properties later
data class PatientSummaryBackClicked(
    val patientUuid: UUID,
    val screenCreatedTimestamp: Instant
) : PatientSummaryEvent() {
  override val analyticsName = "Patient Summary:Back Clicked"
}

// TODO(vs): 2020-01-16 Consider whether these should be moved to the effect handler as properties later
data class PatientSummaryDoneClicked(val patientUuid: UUID) : PatientSummaryEvent() {
  override val analyticsName = "Patient Summary:Done Clicked"
}

data class CurrentFacilityLoaded(val facility: Facility) : PatientSummaryEvent()
