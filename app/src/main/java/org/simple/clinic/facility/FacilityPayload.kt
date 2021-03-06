package org.simple.clinic.facility

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.simple.clinic.location.Coordinates
import org.simple.clinic.patient.SyncStatus
import org.threeten.bp.Instant
import java.util.UUID

@JsonClass(generateAdapter = true)
data class FacilityPayload(

    @Json(name = "id")
    val uuid: UUID,

    @Json(name = "name")
    val name: String,

    @Json(name = "facility_type")
    val facilityType: String?,

    @Json(name = "street_address")
    val streetAddress: String?,

    @Json(name = "village_or_colony")
    val villageOrColony: String?,

    @Json(name = "district")
    val district: String,

    @Json(name = "state")
    val state: String,

    @Json(name = "country")
    val country: String,

    @Json(name = "pin")
    val pinCode: String?,

    @Json(name = "protocol_id")
    val protocolUuid: UUID,

    @Json(name = "facility_group_id")
    val groupUuid: UUID,

    @Json(name = "latitude")
    val locationLatitude: Double?,

    @Json(name = "longitude")
    val locationLongitude: Double?,

    @Json(name = "created_at")
    val createdAt: Instant,

    @Json(name = "updated_at")
    val updatedAt: Instant,

    @Json(name = "deleted_at")
    val deletedAt: Instant?,

    // TODO(vs): 2020-01-22 Make this non-nullable once the feature is in PROD
    @Json(name = "config")
    val config: FacilityConfig?
) {

  fun toDatabaseModel(syncStatus: SyncStatus): Facility {
    val coordinates = if (locationLatitude != null && locationLongitude != null) {
      Coordinates(
          latitude = locationLatitude,
          longitude = locationLongitude)
    } else {
      null
    }

    return Facility(
        uuid = uuid,
        name = name,
        facilityType = facilityType,
        streetAddress = streetAddress,
        villageOrColony = villageOrColony,
        district = district,
        state = state,
        country = country,
        pinCode = pinCode,
        protocolUuid = protocolUuid,
        groupUuid = groupUuid,
        location = coordinates,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncStatus = syncStatus,
        deletedAt = deletedAt,
        config = config ?: FacilityConfig(diabetesManagementEnabled = false)
    )
  }
}
