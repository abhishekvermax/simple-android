package org.simple.clinic.settings.changelanguage

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Observable
import io.reactivex.rxkotlin.cast
import kotlinx.android.synthetic.main.screen_change_language.view.*
import org.simple.clinic.ReportAnalyticsEvents
import org.simple.clinic.activity.TheActivity
import org.simple.clinic.crash.CrashReporter
import org.simple.clinic.mobius.MobiusDelegate
import org.simple.clinic.router.screen.ScreenRouter
import org.simple.clinic.settings.Language
import org.simple.clinic.settings.SettingsRepository
import org.simple.clinic.util.scheduler.SchedulersProvider
import org.simple.clinic.util.unsafeLazy
import org.simple.clinic.widgets.ItemAdapter
import javax.inject.Inject

class ChangeLanguageScreen(
    context: Context,
    attributeSet: AttributeSet
) : ConstraintLayout(context, attributeSet), ChangeLanguageUi {

  @Inject
  lateinit var schedulersProvider: SchedulersProvider

  @Inject
  lateinit var settingsRepository: SettingsRepository

  @Inject
  lateinit var screenRouter: ScreenRouter

  @Inject
  lateinit var crashReporter: CrashReporter

  private val languagesAdapter = ItemAdapter(ChangeLanguageListItem.DiffCallback())

  private val events: Observable<ChangeLanguageEvent> by unsafeLazy {
    Observable
        .never<ChangeLanguageEvent>()
        .compose(ReportAnalyticsEvents())
        .cast<ChangeLanguageEvent>()
  }

  private val uiRenderer = ChangeLanguageUiRenderer(this)

  private val delegate: MobiusDelegate<ChangeLanguageModel, ChangeLanguageEvent, ChangeLanguageEffect> by unsafeLazy {
    MobiusDelegate(
        events = events,
        defaultModel = ChangeLanguageModel.FETCHING_LANGUAGES,
        init = ChangeLanguageInit(),
        update = ChangeLanguageUpdate(),
        effectHandler = ChangeLanguageEffectHandler.create(
            schedulersProvider = schedulersProvider,
            settingsRepository = settingsRepository,
            uiActions = ChangeLanguageScreenUiActions(screenRouter)
        ),
        modelUpdateListener = uiRenderer::render,
        crashReporter = crashReporter
    )
  }

  override fun onFinishInflate() {
    super.onFinishInflate()
    if (isInEditMode) {
      return
    }

    TheActivity.component.inject(this)

    setupLanguagesList()

    delegate.prepare()
  }

  private fun setupLanguagesList() {
    languagesList.apply {
      setHasFixedSize(true)
      layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
      adapter = languagesAdapter
    }
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

  override fun displayLanguages(supportedLanguages: List<Language>, selectedLanguage: Language?) {
    languagesAdapter.submitList(ChangeLanguageListItem.from(supportedLanguages, selectedLanguage))
  }

  override fun setDoneButtonDisabled() {
    doneButton.isEnabled = false
  }

  override fun setDoneButtonEnabled() {
    doneButton.isEnabled = true
  }
}

private class ChangeLanguageScreenUiActions(private val screenRouter: ScreenRouter) : UiActions {

  override fun goBackToPreviousScreen() {
    screenRouter.pop()
  }
}
