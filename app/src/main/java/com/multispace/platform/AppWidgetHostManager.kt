package com.multispace.platform

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import com.multispace.diagnostics.AppLogger
import com.multispace.domain.model.DiscoveredApp

data class WidgetItemInfo(
  val providerInfo: AppWidgetProviderInfo,
  val label: String,
  val spanX: Int,
  val spanY: Int,
  val minSpanX: Int,
  val minSpanY: Int,
  val maxSpanX: Int,
  val maxSpanY: Int,
  val previewDrawable: Drawable?,
  val iconDrawable: Drawable?,
  val hasConfigure: Boolean
)

data class AppWidgetGroup(
  val app: DiscoveredApp,
  val widgets: List<WidgetItemInfo>
)

class MultiSpaceAppWidgetHost(context: Context, hostId: Int) : AppWidgetHost(context, hostId) {
  override fun onCreateView(
    context: Context,
    appWidgetId: Int,
    appWidget: AppWidgetProviderInfo?
  ): AppWidgetHostView {
    return MultiSpaceAppWidgetHostView(context)
  }
}

class MultiSpaceAppWidgetHostView(context: Context) : AppWidgetHostView(context)

object AppWidgetHostManager {
  const val HOST_ID = 2048
  @Volatile
  private var appWidgetHost: MultiSpaceAppWidgetHost? = null

  fun getHost(context: Context): MultiSpaceAppWidgetHost {
    return appWidgetHost ?: synchronized(this) {
      appWidgetHost ?: MultiSpaceAppWidgetHost(context.applicationContext, HOST_ID).also {
        appWidgetHost = it
      }
    }
  }

  fun startListening(context: Context) {
    try {
      getHost(context).startListening()
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to start listening on AppWidgetHost", e)
    }
  }

  fun stopListening(context: Context) {
    try {
      getHost(context).stopListening()
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to stop listening on AppWidgetHost", e)
    }
  }

  fun allocateAppWidgetId(context: Context): Int {
    return getHost(context).allocateAppWidgetId()
  }

  fun deleteAppWidgetId(context: Context, appWidgetId: Int) {
    try {
      getHost(context).deleteAppWidgetId(appWidgetId)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to delete appWidgetId $appWidgetId", e)
    }
  }

  fun getProviderInfo(context: Context, appWidgetId: Int): AppWidgetProviderInfo? {
    val appWidgetManager = AppWidgetManager.getInstance(context) ?: return null
    return try {
      appWidgetManager.getAppWidgetInfo(appWidgetId)
    } catch (e: Exception) {
      null
    }
  }

  fun createWidgetView(context: Context, appWidgetId: Int, providerInfo: AppWidgetProviderInfo?): AppWidgetHostView {
    return getHost(context).createView(context, appWidgetId, providerInfo)
  }

  fun getWidgetGroupsForApps(context: Context, spaceApps: List<DiscoveredApp>): List<AppWidgetGroup> {
    val appWidgetManager = AppWidgetManager.getInstance(context) ?: return emptyList()
    val providers = try {
      appWidgetManager.installedProviders ?: emptyList()
    } catch (e: Exception) {
      emptyList<AppWidgetProviderInfo>()
    }

    val appsByPackage = spaceApps.associateBy { it.packageName }
    val providersByPackage = providers.groupBy { it.provider.packageName }

    val groups = mutableListOf<AppWidgetGroup>()
    for ((pkg, app) in appsByPackage) {
      val packageProviders = providersByPackage[pkg]
      if (!packageProviders.isNullOrEmpty()) {
        val widgetItems = packageProviders.map { provider ->
          val label = try {
            provider.loadLabel(context.packageManager).toString().ifBlank { app.label }
          } catch (e: Exception) {
            app.label
          }

          val spanX = calculateSpan(provider.minWidth, context)
          val spanY = calculateSpan(provider.minHeight, context)
          val minSpanX = calculateSpan(if (provider.minResizeWidth > 0) provider.minResizeWidth else provider.minWidth, context)
          val minSpanY = calculateSpan(if (provider.minResizeHeight > 0) provider.minResizeHeight else provider.minHeight, context)
          val maxSpanX = if (provider.maxResizeWidth > 0) calculateSpan(provider.maxResizeWidth, context).coerceAtLeast(spanX) else 4
          val maxSpanY = if (provider.maxResizeHeight > 0) calculateSpan(provider.maxResizeHeight, context).coerceAtLeast(spanY) else 4

          val previewDrawable = try {
            provider.loadPreviewImage(context, 0)
          } catch (e: Exception) {
            null
          }
          val iconDrawable = try {
            provider.loadIcon(context, 0)
          } catch (e: Exception) {
            null
          }

          WidgetItemInfo(
            providerInfo = provider,
            label = label,
            spanX = spanX,
            spanY = spanY,
            minSpanX = minSpanX,
            minSpanY = minSpanY,
            maxSpanX = maxSpanX,
            maxSpanY = maxSpanY,
            previewDrawable = previewDrawable,
            iconDrawable = iconDrawable,
            hasConfigure = provider.configure != null
          )
        }
        groups.add(AppWidgetGroup(app = app, widgets = widgetItems))
      }
    }
    return groups.sortedBy { it.app.label.lowercase() }
  }

  private fun calculateSpan(sizePxOrDp: Int, context: Context): Int {
    if (sizePxOrDp <= 0) return 1
    val density = context.resources.displayMetrics.density
    val dp = if (sizePxOrDp > 200) (sizePxOrDp / density).toInt() else sizePxOrDp
    return ((dp + 30) / 70).coerceIn(1, 4)
  }
}
