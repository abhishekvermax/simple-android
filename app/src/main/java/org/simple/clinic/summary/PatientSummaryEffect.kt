package org.simple.clinic.summary

import org.threeten.bp.Instant
import java.util.UUID

sealed class PatientSummaryEffect

data class LoadPatientSummaryProfile(val patientUuid: UUID) : PatientSummaryEffect()

// TODO(vs): 2020-01-15 Revisit this effect once the patient summary migration is done
data class HandleBackClick(
    val patientUuid: UUID,
    val screenCreatedTimestamp: Instant
) : PatientSummaryEffect()

// TODO(vs): 2020-01-16 Revisit this effect once the patient summary migration is done
data class HandleDoneClick(
    val patientUuid: UUID
) : PatientSummaryEffect()

object LoadCurrentFacility : PatientSummaryEffect()
