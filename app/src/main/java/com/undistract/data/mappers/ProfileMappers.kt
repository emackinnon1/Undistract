package com.undistract.data.mappers

import com.undistract.data.entities.ProfileEntity
import com.undistract.data.models.Profile

/**
 * Converts a Profile to a ProfileEntity
 */
fun Profile.toEntity(): ProfileEntity {
    return ProfileEntity(
        id = id,
        name = name,
        appPackageNames = appPackageNames,
        icon = icon
    )
}

/**
 * Converts a ProfileEntity to a Profile
 */
fun ProfileEntity.toProfile(): Profile {
    return Profile(
        id = id,
        name = name,
        appPackageNames = appPackageNames,
        icon = icon
    )
}