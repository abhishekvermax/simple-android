package org.simple.clinic.bloodsugar.entry

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.After
import org.junit.Test
import org.simple.clinic.bloodsugar.BloodSugarRepository
import org.simple.clinic.bloodsugar.entry.BloodSugarValidator.Result.ErrorBloodSugarEmpty
import org.simple.clinic.bloodsugar.entry.BloodSugarValidator.Result.ErrorBloodSugarTooHigh
import org.simple.clinic.bloodsugar.entry.BloodSugarValidator.Result.ErrorBloodSugarTooLow
import org.simple.clinic.facility.FacilityRepository
import org.simple.clinic.mobius.EffectHandlerTestCase
import org.simple.clinic.overdue.AppointmentRepository
import org.simple.clinic.patient.PatientMocker
import org.simple.clinic.patient.PatientRepository
import org.simple.clinic.user.User
import org.simple.clinic.user.UserSession
import org.simple.clinic.util.Just
import org.simple.clinic.util.Optional
import org.simple.clinic.util.TestUserClock
import org.simple.clinic.util.scheduler.TrampolineSchedulersProvider
import org.simple.clinic.util.toUtcInstant
import org.simple.clinic.widgets.ageanddateofbirth.UserInputDateValidator.Result.Invalid.DateIsInFuture
import org.simple.clinic.widgets.ageanddateofbirth.UserInputDateValidator.Result.Invalid.InvalidPattern
import org.threeten.bp.LocalDate
import java.util.UUID

class BloodSugarEntryEffectHandlerTest {

  private val ui = mock<BloodSugarEntryUi>()
  private val userClock = TestUserClock()

  private val userSession = mock<UserSession>()
  private val facilityRepository = mock<FacilityRepository>()
  private val appointmentRepository = mock<AppointmentRepository>()
  private val patientRepository = mock<PatientRepository>()
  private val bloodSugarRepository = mock<BloodSugarRepository>()

  private val effectHandler = BloodSugarEntryEffectHandler(
      ui,
      userSession,
      facilityRepository,
      bloodSugarRepository,
      patientRepository,
      appointmentRepository,
      userClock,
      TrampolineSchedulersProvider()
  ).build()
  private val testCase = EffectHandlerTestCase(effectHandler)

  @After
  fun tearDown() {
    testCase.dispose()
  }

  @Test
  fun `blood sugar error message must be hidden when hide blood sugar error message effect is received`() {
    // when
    testCase.dispatch(HideBloodSugarErrorMessage)

    // then
    verify(ui).hideBloodSugarErrorMessage()
    verifyNoMoreInteractions(ui)
  }

  @Test
  fun `date error message must be hidden when hide date error message effect is received`() {
    // when
    testCase.dispatch(HideDateErrorMessage)

    // then
    verify(ui).hideDateErrorMessage()
    verifyNoMoreInteractions(ui)
  }

  @Test
  fun `blood sugar entry sheet must be dismissed when dismiss effect is received`() {
    // when
    testCase.dispatch(Dismiss)

    // then
    verify(ui).dismiss()
    verifyNoMoreInteractions(ui)
  }

  @Test
  fun `show date entry screen when show date entry screen effect is received`() {
    // when
    testCase.dispatch(ShowDateEntryScreen)

    // then
    verify(ui).showDateEntryScreen()
    verifyNoMoreInteractions(ui)
  }

  @Test
  fun `show blood sugar empty error when show blood sugar validation error effect is received with validation result empty`() {
    // when
    testCase.dispatch(ShowBloodSugarValidationError(ErrorBloodSugarEmpty))

    // then
    verify(ui).showBloodSugarEmptyError()
    verifyNoMoreInteractions(ui)
  }

  @Test
  fun `show blood sugar high error when show blood sugar validation error effect is received with validation result too high`() {
    // when
    testCase.dispatch(ShowBloodSugarValidationError(ErrorBloodSugarTooHigh))

    // then
    verify(ui).showBloodSugarHighError()
    verifyNoMoreInteractions(ui)
  }

  @Test
  fun `show blood sugar low error when show blood sugar validation error effect is received with validation result too low`() {
    // when
    testCase.dispatch(ShowBloodSugarValidationError(ErrorBloodSugarTooLow))

    // then
    verify(ui).showBloodSugarLowError()
    verifyNoMoreInteractions(ui)
  }

  @Test
  fun `show blood sugar entry screen when show blood sugar entry screen effect is received`() {
    // given
    val bloodSugarDate = LocalDate.of(2020, 1, 1)

    // when
    testCase.dispatch(ShowBloodSugarEntryScreen(bloodSugarDate))

    // then
    verify(ui).showBloodSugarEntryScreen()
    verify(ui).showDateOnDateButton(bloodSugarDate)
    verifyNoMoreInteractions(ui)
  }

  @Test
  fun `set date on input fields and date button when prefill date effect is received`() {
    // given
    val entryDate = LocalDate.of(1992, 6, 7)
    userClock.setDate(LocalDate.of(1992, 6, 7))

    // when
    testCase.dispatch(PrefillDate.forNewEntry())

    // then
    testCase.assertOutgoingEvents(DatePrefilled(entryDate))
    verify(ui).setDateOnInputFields("7", "6", "92")
    verify(ui).showDateOnDateButton(entryDate)
    verifyNoMoreInteractions(ui)
  }

  @Test
  fun `show invalid date error when show date validation error is received with validation invalid pattern`() {
    // when
    testCase.dispatch(ShowDateValidationError(InvalidPattern))

    // then
    verify(ui).showInvalidDateError()
    verifyNoMoreInteractions(ui)
  }

  @Test
  fun `show date is in future error when show date validation error is received with validation date is in future`() {
    // when
    testCase.dispatch(ShowDateValidationError(DateIsInFuture))

    // then
    verify(ui).showDateIsInFutureError()
    verifyNoMoreInteractions(ui)
  }

  @Test
  fun `set blood sugar saved result and finish when set blood sugar saved result and finish effect is received`() {
    // when
    testCase.dispatch(SetBloodSugarSavedResultAndFinish)

    // then
    verify(ui).setBloodSugarSavedResultAndFinish()
    verifyNoMoreInteractions(ui)
  }

  @Test
  fun `create new blood sugar entry when create blood sugar entry effect is received`() {
    // given
    val user = PatientMocker.loggedInUser(uuid = UUID.fromString("4844b826-a162-49fe-b92c-962da172e86c"))
    val facility = PatientMocker.facility(uuid = UUID.fromString("f895b54f-ee32-4471-bc0c-a91b80368778"))

    val date = LocalDate.parse("2020-01-02")
    val bloodSugar = PatientMocker.bloodSugar()
    val createNewBloodSugarEntry = CreateNewBloodSugarEntry(
        patientUuid = bloodSugar.patientUuid,
        bloodSugarReading = bloodSugar.reading.value,
        measurementType = bloodSugar.reading.type,
        userEnteredDate = date,
        prefilledDate = date
    )

    whenever(userSession.loggedInUser()).doReturn(Observable.just<Optional<User>>(Just(user)))
    whenever(facilityRepository.currentFacility(user)).doReturn(Observable.just(facility))
    whenever(bloodSugarRepository.saveMeasurement(eq(bloodSugar.reading), eq(bloodSugar.patientUuid), eq(user), eq(facility), eq(date.toUtcInstant(userClock)), any())).doReturn(Single.just(bloodSugar))
    whenever(patientRepository.compareAndUpdateRecordedAt(bloodSugar.patientUuid, date.toUtcInstant(userClock))).doReturn(Completable.complete())
    whenever(appointmentRepository.markAppointmentsCreatedBeforeTodayAsVisited(bloodSugar.patientUuid)).doReturn(Completable.complete())

    // when
    testCase.dispatch(createNewBloodSugarEntry)

    // then
    testCase.assertOutgoingEvents(BloodSugarSaved(createNewBloodSugarEntry.wasDateChanged))
    verifyZeroInteractions(ui)
  }
}
