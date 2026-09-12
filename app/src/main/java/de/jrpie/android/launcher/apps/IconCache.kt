package de.jrpie.android.launcher.apps

import android.app.ActivityManager
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.LruCache
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable


object IconCache {
	//TODO: Get Icon size instead of hard-coding values
	private const val ICON_SIZE = 128
	private const val CACHE_PERCENT = 10
	// I assumed that 40 icons will be enough even in the biggest of displays
	private const val PREPOPULATE_LIMIT = 40

	private lateinit var cache: LruCache<String, Drawable>

	fun initialize(context: Context) {
		val activityManager =
			context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

		val memoryClassBytes =
			activityManager.memoryClass * 1024L * 1024L

		val cacheBytes =
			memoryClassBytes * CACHE_PERCENT / 100

		cache = object : LruCache<String, Drawable>(cacheBytes.toInt()) {
			override fun sizeOf(key: String, value: Drawable): Int {
				return (value as BitmapDrawable).bitmap.allocationByteCount
			}
		}

		Log.d(
			"IconCache",
			"Initialized: max=${cache.maxSize() / 1024f / 1024f} MB"
		)
	}

	fun clear() {
		cache.evictAll()
	}

	fun getIcon(context: Context, appInfo: AppInfo): Drawable {
		val key = "${appInfo.packageName}:${appInfo.user}"

		cache.get(key)?.let {
			return it
		}

		val icon = fetchIcon(context, appInfo)
		//TODO: Fix grayscale
		if (icon != null) {
			val processedIcon = processIconSize(context, icon)
			cache.put(key, processedIcon)
			return processedIcon
		}

		return Color.TRANSPARENT.toDrawable()
	}

	fun prePopulate(
		context: Context,
		activityInfo: LauncherActivityInfo
	) {

		if (cache.snapshot().size >= PREPOPULATE_LIMIT) {
			return
		}

		val appInfo = AppInfo(
			activityInfo.applicationInfo.packageName,
			activityInfo.name,
			activityInfo.user.hashCode()
		)

		val key = "${appInfo.packageName}:${appInfo.user}"

		if (cache.get(key) != null) {
			return
		}

		val icon = fetchIcon(context, appInfo)

		if (icon != null) {
			val processedIcon = processIconSize(context, icon)
			cache.put(key, processedIcon)
		}
	}

	private fun fetchIcon(
		context: Context,
		appInfo: AppInfo
	): Drawable? {
		val activityInfo = appInfo.getLauncherActivityInfo(context)
		return activityInfo?.getBadgedIcon(0)
	}

	private fun processIconSize(
		context: Context,
		icon: Drawable
	): Drawable {
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

		val scaledBitmap = Bitmap.createScaledBitmap(
			bitmap,
			ICON_SIZE,
			ICON_SIZE,
			true
		)

		return scaledBitmap.toDrawable(context.resources)
	}
}
