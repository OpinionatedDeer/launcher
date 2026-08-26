package de.jrpie.android.launcher.apps

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import java.util.concurrent.ConcurrentHashMap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.createBitmap


object IconCache {

    private val cache = ConcurrentHashMap<String, Drawable>()

    fun clear() {
        cache.clear()
    }

    fun getIcon(context: Context, appInfo: AppInfo): Drawable {
        val key = "${appInfo.packageName}:${appInfo.user}"
        val cachedIcon = cache[key]
        if (cachedIcon != null) {
            return cachedIcon
        }

        val icon = fetchIcon(context, appInfo)
        if (icon != null) {
            val processedIcon = processIconSize(context, icon)
            cache[key] = processedIcon
            return processedIcon
        }

        return Color.TRANSPARENT.toDrawable()
    }
    //Here to reduce the lag for initial load of app
    fun prePopulate(context: Context, activityInfo: LauncherActivityInfo) {
        val appInfo = AppInfo(
            activityInfo.applicationInfo.packageName,
            activityInfo.name,
            activityInfo.user.hashCode()
        )
        val key = "${appInfo.packageName}:${appInfo.user}"
        if (cache.get(key) == null) {
            val icon = fetchIcon(context, appInfo)
            if (icon != null) {
                val processedIcon = processIconSize(context, icon)
                cache.put(key, processedIcon)
            }
        }
    }

    private fun fetchIcon(context: Context, appInfo: AppInfo): Drawable? {
        val activityInfo = appInfo.getLauncherActivityInfo(context)
        return activityInfo?.getBadgedIcon(0)
    }

    private fun processIconSize(context: Context, icon: Drawable): Drawable {
        //I have once again hardcoded a random value which I thought should be good 
        //TODO: Figure out a better way to put value
        val targetSize = 128
        
        val bitmap = if (icon is BitmapDrawable) {
            icon.bitmap
        } else {
            val bmp = createBitmap(
                icon.intrinsicWidth.coerceAtLeast(1),
                icon.intrinsicHeight.coerceAtLeast(1)
            )
            val canvas = Canvas(bmp)
            icon.setBounds(0, 0, canvas.width, canvas.height)
            icon.draw(canvas)
            bmp
        }

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, true)
        return scaledBitmap.toDrawable(context.resources)
    }
}
