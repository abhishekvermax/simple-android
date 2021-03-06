package org.simple.clinic.bloodsugar.history

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import android.util.AttributeSet
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.jakewharton.rxbinding3.view.detaches
import io.reactivex.Observable
import io.reactivex.rxkotlin.cast
import io.reactivex.rxkotlin.ofType
import kotlinx.android.synthetic.main.screen_blood_sugar_history.view.*
import org.simple.clinic.R
import org.simple.clinic.ReportAnalyticsEvents
import org.simple.clinic.bloodsugar.BloodSugarMeasurement
import org.simple.clinic.bloodsugar.entry.BloodSugarEntrySheet
import org.simple.clinic.bloodsugar.history.adapter.BloodSugarHistoryListItem
import org.simple.clinic.bloodsugar.history.adapter.BloodSugarHistoryListItemDiffCallback
import org.simple.clinic.bloodsugar.history.adapter.NewBloodSugarClicked
import org.simple.clinic.bloodsugar.selection.type.BloodSugarTypePickerSheet
import org.simple.clinic.di.injector
import org.simple.clinic.mobius.MobiusDelegate
import org.simple.clinic.patient.DateOfBirth
import org.simple.clinic.patient.Gender
import org.simple.clinic.patient.Patient
import org.simple.clinic.patient.displayLetterRes
import org.simple.clinic.platform.crash.CrashReporter
import org.simple.clinic.router.screen.ActivityResult
import org.simple.clinic.router.screen.ScreenRouter
import org.simple.clinic.summary.TYPE_PICKER_SHEET
import org.simple.clinic.util.UserClock
import org.simple.clinic.util.unsafeLazy
import org.simple.clinic.widgets.DividerItemDecorator
import org.simple.clinic.widgets.ItemAdapter
import org.simple.clinic.widgets.ScreenDestroyed
import org.simple.clinic.widgets.dp
import org.threeten.bp.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named

class BloodSugarHistoryScreen(
    context: Context,
    attrs: AttributeSet
) : ConstraintLayout(context, attrs), BloodSugarHistoryScreenUi, BloodSugarHistoryScreenUiActions {

  @Inject
  lateinit var activity: AppCompatActivity

  @Inject
  lateinit var userClock: UserClock

  @field:[Inject Named("date_for_measurement_history")]
  lateinit var dateFormatter: DateTimeFormatter

  @field:[Inject Named("time_for_measurement_history")]
  lateinit var timeFormatter: DateTimeFormatter

  @Inject
  lateinit var screenRouter: ScreenRouter

  @Inject
  lateinit var effectHandlerFactory: BloodSugarHistoryScreenEffectHandler.Factory

  @Inject
  lateinit var crashReporter: CrashReporter

  private val bloodSugarHistoryAdapter = ItemAdapter(BloodSugarHistoryListItemDiffCallback())

  private val events: Observable<BloodSugarHistoryScreenEvent> by unsafeLazy {
    addNewBloodSugarClicked()
        .compose(ReportAnalyticsEvents())
        .cast<BloodSugarHistoryScreenEvent>()
  }

  private val uiRenderer = BloodSugarHistoryScreenUiRenderer(this)

  private val delegate: MobiusDelegate<BloodSugarHistoryScreenModel, BloodSugarHistoryScreenEvent, BloodSugarHistoryScreenEffect> by unsafeLazy {
    val screenKey = screenRouter.key<BloodSugarHistoryScreenKey>(this)
    MobiusDelegate(
        events = events,
        defaultModel = BloodSugarHistoryScreenModel.create(screenKey.patientUuid),
        init = BloodSugarHistoryScreenInit(),
        update = BloodSugarHistoryScreenUpdate(),
        effectHandler = effectHandlerFactory.create(this).build(),
        modelUpdateListener = uiRenderer::render,
        crashReporter = crashReporter
    )
  }

  override fun onFinishInflate() {
    super.onFinishInflate()
    if (isInEditMode) {
      return
    }
    context.injector<BloodSugarHistoryScreenInjector>().inject(this)

    val screenDestroys: Observable<ScreenDestroyed> = detaches().map { ScreenDestroyed() }
    openEntrySheetAfterTypeIsSelected(screenDestroys)

    delegate.prepare()

    handleToolbarBackClick()
    setupBloodSugarHistoryList()
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    delegate.start()
  }

  override fun onDetachedFromWindow() {
    delegate.stop()
    super.onDetachedFromWindow()
  }

  override fun onSaveInstanceState(): Parcelable? {
    return delegate.onSaveInstanceState(super.onSaveInstanceState())
  }

  override fun onRestoreInstanceState(state: Parcelable?) {
    super.onRestoreInstanceState(delegate.onRestoreInstanceState(state))
  }

  override fun showPatientInformation(patient: Patient) {
    val ageValue = DateOfBirth.fromPatient(patient, userClock).estimateAge(userClock)
    displayNameGenderAge(patient.fullName, patient.gender, ageValue)
  }

  override fun showBloodSugarHistory(bloodSugars: List<BloodSugarMeasurement>) {
    bloodSugarHistoryAdapter.submitList(BloodSugarHistoryListItem.from(
        bloodSugars,
        userClock,
        dateFormatter,
        timeFormatter
    ))
  }

  override fun openBloodSugarEntrySheet(patientUuid: UUID) {
    val intent = BloodSugarTypePickerSheet.intent(context)
    activity.startActivityForResult(intent, TYPE_PICKER_SHEET)
  }

  private fun displayNameGenderAge(name: String, gender: Gender, age: Int) {
    val genderLetter = resources.getString(gender.displayLetterRes)
    toolbar.title = resources.getString(R.string.bloodsugarhistory_toolbar_title, name, genderLetter, age.toString())
  }

  private fun handleToolbarBackClick() {
    toolbar.setNavigationOnClickListener {
      screenRouter.pop()
    }
  }

  private fun setupBloodSugarHistoryList() {
    val dividerMargin = 8.dp
    val divider = DividerItemDecorator(context = context, marginStart = dividerMargin, marginEnd = dividerMargin)

    bloodSugarHistoryList.apply {
      setHasFixedSize(true)
      layoutManager = LinearLayoutManager(context)
      addItemDecoration(divider)
      adapter = bloodSugarHistoryAdapter
    }
  }

  @SuppressLint("CheckResult")
  private fun openEntrySheetAfterTypeIsSelected(onDestroys: Observable<ScreenDestroyed>) {
    screenRouter.streamScreenResults()
        .ofType<ActivityResult>()
        .filter { it.requestCode == TYPE_PICKER_SHEET && it.succeeded() && it.data != null }
        .takeUntil(onDestroys)
        .map { it.data!! }
        .subscribe(::showBloodSugarEntrySheet)
  }

  private fun showBloodSugarEntrySheet(intent: Intent) {
    val screenKey = screenRouter.key<BloodSugarHistoryScreenKey>(this)
    val patientUuid = screenKey.patientUuid

    val intentForNewBloodSugar = BloodSugarEntrySheet.intentForNewBloodSugar(
        context,
        patientUuid,
        BloodSugarTypePickerSheet.selectedBloodSugarType(intent)
    )
    activity.startActivity(intentForNewBloodSugar)
  }

  private fun addNewBloodSugarClicked(): Observable<BloodSugarHistoryScreenEvent> {
    return bloodSugarHistoryAdapter
        .itemEvents
        .ofType<NewBloodSugarClicked>()
        .map { AddNewBloodSugarClicked }
  }
}
