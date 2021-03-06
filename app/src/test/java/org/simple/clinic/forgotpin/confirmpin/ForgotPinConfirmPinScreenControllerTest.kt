package org.simple.clinic.forgotpin.confirmpin

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.clearInvocations
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.simple.clinic.facility.FacilityRepository
import org.simple.clinic.patient.PatientMocker
import org.simple.clinic.user.User
import org.simple.clinic.user.UserSession
import org.simple.clinic.user.clearpatientdata.SyncAndClearPatientData
import org.simple.clinic.user.resetpin.ResetPinResult
import org.simple.clinic.user.resetpin.ResetPinResult.NetworkError
import org.simple.clinic.user.resetpin.ResetPinResult.Success
import org.simple.clinic.user.resetpin.ResetPinResult.UnexpectedError
import org.simple.clinic.user.resetpin.ResetPinResult.UserNotFound
import org.simple.clinic.user.resetpin.ResetUserPin
import org.simple.clinic.util.Just
import org.simple.clinic.util.Optional
import org.simple.clinic.util.RxErrorsRule
import org.simple.clinic.widgets.UiEvent
import java.util.UUID

class ForgotPinConfirmPinScreenControllerTest {

  @get:Rule
  val rxErrorsRule = RxErrorsRule()

  private val uiEvents = PublishSubject.create<UiEvent>()

  private val userSession = mock<UserSession>()
  private val facilityRepository = mock<FacilityRepository>()
  private val screen = mock<ForgotPinConfirmPinScreen>()
  private val resetUserPin = mock<ResetUserPin>()
  private val syncAndClearPatientData = mock<SyncAndClearPatientData>()

  private val loggedInUser = PatientMocker.loggedInUser(uuid = UUID.fromString("324d7648-e2a5-4192-831f-533b81181dc2"))
  private val facility = PatientMocker.facility()

  private val controller = ForgotPinConfirmPinScreenController(
      userSession = userSession,
      facilityRepository = facilityRepository,
      resetUserPin = resetUserPin,
      syncAndClearPatientData = syncAndClearPatientData
  )

  // FIXME 02-08-2019 : Fix tests with unexpected errors are passing even when stubs are not passed
  // We use onErrorReturn in one of the chains to wrap unexpected errors. However, when we forget
  // to stub certain calls in Mockito (UserSession#updateLoggedInStatusForUser), the tests still
  // pass (even though they shouldn't) because we are using onErrorReturn to wrap errors from
  // upstream. This needs to be fixed so that tests behave properly.

  @Before
  fun setUp() {
    whenever(userSession.loggedInUser()) doReturn Observable.just<Optional<User>>(Just(loggedInUser))
    whenever(facilityRepository.currentFacility(loggedInUser)) doReturn Observable.just(facility)

    uiEvents
        .compose(controller)
        .subscribe { it.invoke(screen) }
  }

  @Test
  fun `on start, the logged in user's full name must be shown`() {
    // when
    uiEvents.onNext(ForgotPinConfirmPinScreenCreated("1111"))

    // then
    verify(screen).showUserName(loggedInUser.fullName)
  }

  @Test
  fun `on start, the current selected facility should be shown`() {
    // when
    uiEvents.onNext(ForgotPinConfirmPinScreenCreated("1111"))

    // then
    verify(screen).showFacility(facility.name)
  }

  @Test
  fun `when submitting a PIN that does not match the previous PIN, an error should be shown`() {
    // given
    val originalPin = "0000"
    whenever(syncAndClearPatientData.run()) doReturn Completable.complete()
    whenever(resetUserPin.resetPin(originalPin)) doReturn Single.just<ResetPinResult>(Success)

    // then
    uiEvents.onNext(ForgotPinConfirmPinScreenCreated(originalPin))

    uiEvents.onNext(ForgotPinConfirmPinSubmitClicked("1234"))
    verify(screen).showPinMismatchedError()

    clearInvocations(screen)

    uiEvents.onNext(ForgotPinConfirmPinSubmitClicked("5678"))
    verify(screen).showPinMismatchedError()
  }

  @Test
  fun `when PIN is changed, any errors must be hidden`() {
    // when
    uiEvents.onNext(ForgotPinConfirmPinTextChanged("1"))
    uiEvents.onNext(ForgotPinConfirmPinTextChanged("11"))
    uiEvents.onNext(ForgotPinConfirmPinTextChanged("111"))

    // then
    verify(screen, times(3)).hideError()
  }

  @Test
  fun `when a valid PIN is submitted, the local patient data must be synced and then cleared`() {
    // given
    val pin = "0000"
    whenever(syncAndClearPatientData.run()) doReturn Completable.complete()

    // when
    uiEvents.onNext(ForgotPinConfirmPinScreenCreated(pin))
    uiEvents.onNext(ForgotPinConfirmPinSubmitClicked(pin))

    // then
    verify(syncAndClearPatientData).run()
  }

  @Test
  fun `when the sync fails, it must show an unexpected error`() {
    // given
    whenever(syncAndClearPatientData.run()) doReturn Completable.error(RuntimeException())

    // when
    val pin = "0000"
    uiEvents.onNext(ForgotPinConfirmPinScreenCreated(pin))
    uiEvents.onNext(ForgotPinConfirmPinSubmitClicked(pin))

    // then
    verify(screen).showUnexpectedError()
  }

  @Test
  fun `when a valid PIN is submitted and sync succeeds, it must raise the Reset PIN request`() {
    // given
    val pin = "0000"
    whenever(resetUserPin.resetPin(pin)) doReturn Single.just<ResetPinResult>(Success)
    whenever(syncAndClearPatientData.run()) doReturn Completable.complete()
    whenever(userSession.updateLoggedInStatusForUser(loggedInUser.uuid, User.LoggedInStatus.RESETTING_PIN)) doReturn Completable.complete()

    // when
    uiEvents.onNext(ForgotPinConfirmPinScreenCreated(pin))
    uiEvents.onNext(ForgotPinConfirmPinSubmitClicked(pin))

    // then
    verify(syncAndClearPatientData).run()
    verify(resetUserPin).resetPin(pin)
  }

  @Test
  fun `when an invalid PIN is submitted, it must not attempt to raise the Reset PIN request`() {
    // when
    uiEvents.onNext(ForgotPinConfirmPinScreenCreated("0000"))
    uiEvents.onNext(ForgotPinConfirmPinSubmitClicked("1234"))

    // then
    verify(resetUserPin, never()).resetPin(any())
    verify(syncAndClearPatientData, never()).run()
  }

  @Test
  fun `when a valid PIN is submitted, the progress must be shown`() {
    // when
    val pin = "0000"
    whenever(syncAndClearPatientData.run()) doReturn Completable.complete()

    uiEvents.onNext(ForgotPinConfirmPinScreenCreated(pin))
    uiEvents.onNext(ForgotPinConfirmPinSubmitClicked(pin))

    // then
    verify(screen).showProgress()
  }

  @Test
  fun `when the forgot PIN call fails with a network error, the error must be shown`() {
    // given
    val pin = "0000"
    whenever(syncAndClearPatientData.run()) doReturn Completable.complete()
    whenever(userSession.updateLoggedInStatusForUser(loggedInUser.uuid, User.LoggedInStatus.RESETTING_PIN)) doReturn Completable.complete()
    whenever(resetUserPin.resetPin(pin)) doReturn Single.just<ResetPinResult>(NetworkError)

    // when
    uiEvents.onNext(ForgotPinConfirmPinScreenCreated(pin))
    uiEvents.onNext(ForgotPinConfirmPinSubmitClicked(pin))

    // then
    verify(screen).showNetworkError()
  }

  @Test
  fun `when the forgot PIN call fails with an unexpected error, the error must be shown`() {
    // given
    val pin = "0000"
    whenever(syncAndClearPatientData.run()) doReturn Completable.complete()
    whenever(resetUserPin.resetPin(pin)) doReturn Single.just<ResetPinResult>(UnexpectedError(RuntimeException()))

    // when
    uiEvents.onNext(ForgotPinConfirmPinScreenCreated(pin))
    uiEvents.onNext(ForgotPinConfirmPinSubmitClicked(pin))

    // then
    verify(screen).showUnexpectedError()
  }

  @Test
  fun `when the forgot PIN call fails with a user not found error, the error must be shown`() {
    // given
    val pin = "0000"
    whenever(syncAndClearPatientData.run()) doReturn Completable.complete()
    whenever(resetUserPin.resetPin(pin)) doReturn Single.just<ResetPinResult>(UserNotFound)

    // when
    uiEvents.onNext(ForgotPinConfirmPinScreenCreated(pin))
    uiEvents.onNext(ForgotPinConfirmPinSubmitClicked(pin))

    // then
    verify(screen).showUnexpectedError()
  }

  @Test
  fun `when the forgot PIN call succeeds, the home screen must be opened`() {
    // given
    val pin = "0000"
    whenever(syncAndClearPatientData.run()) doReturn Completable.complete()
    whenever(userSession.updateLoggedInStatusForUser(loggedInUser.uuid, User.LoggedInStatus.RESETTING_PIN)) doReturn Completable.complete()
    whenever(resetUserPin.resetPin(pin)) doReturn Single.just<ResetPinResult>(Success)

    // then
    uiEvents.onNext(ForgotPinConfirmPinScreenCreated(pin))
    uiEvents.onNext(ForgotPinConfirmPinSubmitClicked(pin))

    // then
    verify(screen).goToHomeScreen()
  }

  @Test
  fun `when resetting the PIN and data gets cleared, the user logged in status must be set to RESETTING_PIN`() {
    // given
    whenever(syncAndClearPatientData.run()) doReturn Completable.complete()

    // when
    uiEvents.onNext(ForgotPinConfirmPinScreenCreated("0000"))
    uiEvents.onNext(ForgotPinConfirmPinSubmitClicked("0000"))

    // then
    verify(userSession).updateLoggedInStatusForUser(loggedInUser.uuid, User.LoggedInStatus.RESETTING_PIN)
  }
}
