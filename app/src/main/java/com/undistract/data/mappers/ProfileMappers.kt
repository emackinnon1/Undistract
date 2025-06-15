package com.undistract.data.mappers

import com.undistract.data.models.Profile
import com.undistract.data.room.ProfileEntity

fun ProfileEntity.toDomain(): Profile =
    Profile(id = id, name = name, appPackageNames = appPackageNames, icon = icon)

fun Profile.toEntity(): ProfileEntity =
    ProfileEntity(id = id, name = name, appPackageNames = appPackageNames, icon = icon)