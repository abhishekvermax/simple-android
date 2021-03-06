package org.simple.clinic.summary.medicalhistory

import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.ObservableTransformer
import io.reactivex.rxkotlin.ofType
import io.reactivex.rxkotlin.withLatestFrom
import org.simple.clinic.ReplayUntilScreenIsDestroyed
import org.simple.clinic.ReportAnalyticsEvents
import org.simple.clinic.facility.Facility
import org.simple.clinic.facility.FacilityRepository
import org.simple.clinic.medicalhistory.MedicalHistoryRepository
import org.simple.clinic.user.UserSession
import org.simple.clinic.util.UtcClock
import org.simple.clinic.util.filterAndUnwrapJust
import org.simple.clinic.widgets.ScreenCreated
import org.simple.clinic.widgets.UiEvent
import org.threeten.bp.Instant
import java.util.UUID

typealias Ui = MedicalHistorySummaryUi

typealias UiChange = (Ui) -> Unit

class MedicalHistorySummaryUiController @AssistedInject constructor(
    @Assisted private val patientUuid: UUID,
    private val medicalHistoryRepository: MedicalHistoryRepository,
    private val userSession: UserSession,
    private val facilityRepository: FacilityRepository,
    private val clock: UtcClock
) : ObservableTransformer<UiEvent, UiChange> {

  @AssistedInject.Factory
  interface Factory {
    fun create(patientUuid: UUID): MedicalHistorySummaryUiController
  }

  override fun apply(events: Observable<UiEvent>): ObservableSource<UiChange> {
    val replayedEvents = ReplayUntilScreenIsDestroyed(events)
        .compose(ReportAnalyticsEvents())
        .replay()

    return Observable.merge(
        displayMedicalHistory(replayedEvents),
        updateMedicalHistory(replayedEvents),
        setupViewForDiabetesManagement(replayedEvents)
    )
  }

  private fun displayMedicalHistory(events: Observable<UiEvent>): Observable<UiChange> {
    return events
        .ofType<ScreenCreated>()
        .switchMap { medicalHistoryRepository.historyForPatientOrDefault(patientUuid) }
        .map { { ui: Ui -> ui.populateMedicalHistory(it) } }
  }

  private fun updateMedicalHistory(events: Observable<UiEvent>): Observable<UiChange> {
    val medicalHistories = medicalHistoryRepository.historyForPatientOrDefault(patientUuid)

    return events.ofType<SummaryMedicalHistoryAnswerToggled>()
        .withLatestFrom(medicalHistories)
        .map { (toggleEvent, medicalHistory) -> medicalHistory.answered(toggleEvent.question, toggleEvent.answer) }
        .flatMap { medicalHistory ->
          medicalHistoryRepository
              .save(medicalHistory, Instant.now(clock))
              .andThen(Observable.never<UiChange>())
        }
  }

  private fun setupViewForDiabetesManagement(events: Observable<UiEvent>): Observable<UiChange> {
    return events
        .ofType<ScreenCreated>()
        .switchMap { currentFacility() }
        .map { it.config.diabetesManagementEnabled }
        .map { diabetesManagementEnabled ->
          { ui: Ui ->
            if (diabetesManagementEnabled) {
              ui.showDiagnosisView()
              ui.hideDiabetesHistorySection()
            } else {
              ui.hideDiagnosisView()
              ui.showDiabetesHistorySection()
            }
          }
        }
  }

  private fun currentFacility(): Observable<Facility> {
    return userSession
        .loggedInUser()
        .filterAndUnwrapJust()
        .flatMap(facilityRepository::currentFacility)
        .take(1)
  }
}
