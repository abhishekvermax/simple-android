package org.simple.clinic.protocolv2.sync

import com.f2prateek.rx.preferences2.Preference
import io.reactivex.Completable
import io.reactivex.Single
import org.simple.clinic.protocolv2.ProtocolConfig
import org.simple.clinic.protocolv2.ProtocolRepository
import org.simple.clinic.protocolv2.ProtocolSyncApiV2
import org.simple.clinic.sync.ModelSync
import org.simple.clinic.sync.SyncCoordinator
import org.simple.clinic.util.Optional
import javax.inject.Inject
import javax.inject.Named

class ProtocolSync @Inject constructor(
    private val syncCoordinator: SyncCoordinator,
    private val repository: ProtocolRepository,
    private val syncApi: ProtocolSyncApiV2,
    private val configProvider: Single<ProtocolConfig>,
    @Named("last_protocol_pull_token") private val lastPullToken: Preference<Optional<String>>
) : ModelSync {

  override fun sync(): Completable = pull()

  override fun push(): Completable = Completable.complete()

  override fun pull(): Completable {
    return configProvider
        .flatMapCompletable { config ->
          if (config.isProtocolDrugSyncEnabled) {
            syncCoordinator.pull(repository, lastPullToken, syncApi::pull)
          } else {
            Completable.complete()
          }
        }
  }
}
