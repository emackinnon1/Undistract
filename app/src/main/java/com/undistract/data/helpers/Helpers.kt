package com.undistract.data.helpers

import androidx.room.TypeConverter
import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat
import com.undistract.data.entities.AppInfoEntity
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable

class StringListConverter {
    @TypeConverter
    fun fromList(list: List<String>): String = list.joinToString(",")

    @TypeConverter
    fun toList(data: String): List<String> =
        if (data.isEmpty()) emptyList() else data.split(",")
}

fun AppInfoEntity.loadIcon(context: Context): Drawable? {
    val resId = context.resources.getIdentifier(iconResName, "drawable", context.packageName)
    return if (resId != 0) ResourcesCompat.getDrawable(context.resources, resId, null) else null
}

fun AppInfoEntity.loadBitmap(context: Context): Bitmap? {
    val drawable = loadIcon(context)
    return (drawable as? BitmapDrawable)?.bitmap
}