package org.simple.clinic.encounter.sync

import com.f2prateek.rx.preferences2.Preference
import io.reactivex.Completable
import io.reactivex.Single
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.rules.RuleChain
import org.simple.clinic.TestClinicApp
import org.simple.clinic.TestData
import org.simple.clinic.encounter.Encounter
import org.simple.clinic.encounter.EncounterRepository
import org.simple.clinic.patient.SyncStatus
import org.simple.clinic.rules.ServerAuthenticationRule
import org.simple.clinic.sync.BaseSyncCoordinatorAndroidTest
import org.simple.clinic.sync.BatchSize
import org.simple.clinic.sync.DataPushResponse
import org.simple.clinic.sync.SyncConfig
import org.simple.clinic.sync.SyncGroup
import org.simple.clinic.sync.SyncInterval
import org.simple.clinic.sync.SynceableRepository
import org.simple.clinic.util.Optional
import org.simple.clinic.util.RxErrorsRule
import javax.inject.Inject
import javax.inject.Named

@Ignore
class EncounterSyncAndroidTest : BaseSyncCoordinatorAndroidTest<Encounter, EncounterPayload>() {

  @Inject
  lateinit var encounterSync: EncounterSync

  @Inject
  lateinit var repository: EncounterRepository

  @Inject
  lateinit var testData: TestData

  @Inject
  lateinit var encounterSyncApi: EncounterSyncApi

  @Inject
  @field:Named("last_encounter_pull_token")
  lateinit var lastPullToken: Preference<Optional<String>>

  private val configProvider = Single.just(SyncConfig(
      syncInterval = SyncInterval.FREQUENT,
      batchSize = BatchSize.VERY_SMALL,
      syncGroup = SyncGroup.FREQUENT))

  @get:Rule
  val ruleChain = RuleChain
      .outerRule(ServerAuthenticationRule())
      .around(RxErrorsRule())!!

  @Before
  fun setUp() {
    TestClinicApp.appComponent().inject(this)
  }

  override fun push(): Completable = encounterSync.push()

  override fun pull(): Completable = encounterSync.pull()

  override fun repository(): SynceableRepository<Encounter, EncounterPayload> = repository

  override fun generateRecord(syncStatus: SyncStatus): Encounter = testData.encounter(syncStatus = syncStatus)

  override fun generatePayload(): EncounterPayload = testData.encounterPayload()

  override fun lastPullToken(): Preference<Optional<String>> = lastPullToken
  
  override fun pushNetworkCall(payloads: List<EncounterPayload>): Single<DataPushResponse> =
      encounterSyncApi.push(EncounterPushRequest(payloads))

  override fun batchSize(): BatchSize = configProvider.blockingGet().batchSize
}