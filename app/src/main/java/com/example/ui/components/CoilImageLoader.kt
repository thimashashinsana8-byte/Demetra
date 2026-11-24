package com.example.ui.components

import android.content.Context
import coil.Coil
import coil.ImageLoader
import coil.memory.MemoryCache
import coil.request.ImageRequest
import com.example.BuildConfig
import com.example.R
import com.google.android.gms.maps.MapsInitializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

private val imageLoaderConfigured = AtomicBoolean(false)
private val imagesPrewarmed = AtomicBoolean(false)
private val mapsPrewarmed = AtomicBoolean(false)

/**
 * Installs a tuned singleton Coil ImageLoader once at startup:
 * - Caps in-memory decoded bitmap cache at 20% of app heap for smooth scrolling without GC spikes
 * - Enables hardware bitmaps and disables implicit crossfades for instant frame presentation
 */
fun configureDemetraImageLoader(context: Context) {
    if (!imageLoaderConfigured.compareAndSet(false, true)) return
    val appContext = context.applicationContext
    val loader = ImageLoader.Builder(appContext)
        .memoryCache {
            MemoryCache.Builder(appContext)
                .maxSizePercent(0.20)
                .build()
        }
        .allowHardware(true)
        .crossfade(false)
        .respectCacheHeaders(false)
        .build()
    Coil.setImageLoader(loader)
}

/**
 * Pre-decodes core brand and food category thumbnails on IO during the splash window
 * so dashboards and lists render with zero first-frame image decode stutter.
 */
suspend fun prewarmDemetraImages(context: Context) {
    if (!imagesPrewarmed.compareAndSet(false, true)) return
    val appContext = context.applicationContext
    configureDemetraImageLoader(appContext)
    withContext(Dispatchers.IO) {
        val loader = Coil.imageLoader(appContext)
        val targets = listOf(
            R.drawable.img_demetra_logo to 192,
            R.drawable.img_food_meals to 220,
            R.drawable.img_food_bakery to 220,
            R.drawable.img_food_produce to 220,
            R.drawable.img_food_shorteats to 220
        )
        targets.forEach { (resId, sizePx) ->
            loader.enqueue(
                ImageRequest.Builder(appContext)
                    .data(resId)
                    .size(sizePx)
                    .allowHardware(true)
                    .crossfade(false)
                    .build()
            )
        }
    }
}

/**
 * Initializes the Google Maps renderer ahead of time when a Maps API key is configured,
 * avoiding a synchronous Play Services stall when opening the District Map tab.
 */
fun prewarmMapsRenderer(context: Context) {
    val key = BuildConfig.MAPS_API_KEY.trim()
    val hasConfiguredKey = key.isNotEmpty() &&
        key != "YOUR_GOOGLE_MAPS_API_KEY" &&
        !key.startsWith("MAPS_API_KEY_DEFAULT")
    if (!hasConfiguredKey) return
    if (!mapsPrewarmed.compareAndSet(false, true)) return
    runCatching {
        MapsInitializer.initialize(context.applicationContext, MapsInitializer.Renderer.LATEST, null)
    }
}
