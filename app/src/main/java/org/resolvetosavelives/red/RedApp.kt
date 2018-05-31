package org.resolvetosavelives.red

import android.app.Application
import com.gabrielittner.threetenbp.LazyThreeTen
import org.resolvetosavelives.red.di.AppComponent
import timber.log.Timber

abstract class RedApp : Application() {

  companion object {
    lateinit var appComponent: AppComponent
  }

  override fun onCreate() {
    super.onCreate()

    if (BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())
    }
    LazyThreeTen.init(this)
    appComponent = buildDaggerGraph()
  }

  abstract fun buildDaggerGraph(): AppComponent
}
