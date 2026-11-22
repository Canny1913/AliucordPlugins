package com.github.canny1913

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.AssetManager
import android.content.res.Resources
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import androidx.annotation.StyleRes
import com.aliucord.Constants
import com.aliucord.entities.Plugin
import java.lang.reflect.Field
import java.lang.reflect.Method

class PluginContext(val ctx: Context, @StyleRes themeId: Int, val plugin: Plugin) : ContextThemeWrapper(ctx, themeId) {
    private var mergedResources: Resources? = null

    init {
        if (plugin.resources != null) construct()

    }


    constructor(ctx: Activity, plugin: Plugin) : this(ctx, ctx.themeResId, plugin)

    constructor(ctx: Context, plugin: Plugin) : this(ctx, 0, plugin)

    override fun getResources(): Resources {
        return mergedResources ?: super.getResources()
    }

    override fun getClassLoader(): ClassLoader? {
        return plugin.javaClass.classLoader
    }

    override fun getSystemService(name: String): Any? {
        if (name == LAYOUT_INFLATER_SERVICE) {
            val systemService = super.getSystemService(name) as LayoutInflater
            return systemService.cloneInContext(this)
        }
        return super.getSystemService(name)
    }

    fun construct() {
        val assetManager = AssetManager::class.java.getDeclaredConstructor().newInstance()
        val assetPathAddMethod: Method = AssetManager::class.java.getMethod("addAssetPath", String::class.java).apply { isAccessible = true }

        // Host application APK
        assetPathAddMethod.invoke(
            assetManager,
            ctx.applicationInfo.sourceDir
        )

        // Host application split APKs, if applicable
        ctx.applicationInfo.splitSourceDirs?.forEach {
            assetPathAddMethod.invoke(assetManager, it)
        }

        // Plugin APK
        assetPathAddMethod.invoke(
            assetManager,
            Constants.PLUGINS_PATH + "/" + plugin.__filename + ".zip"
        )

        @Suppress("DEPRECATION")
        this.mergedResources = Resources(assetManager, ctx.resources.displayMetrics, ctx.resources.configuration)
    }
}

@SuppressLint("PrivateApi")
val themeResField: Field = ContextThemeWrapper::class.java.getDeclaredField("mThemeResource").apply { isAccessible = true }

var ContextThemeWrapper.themeResId: Int
    get() = themeResField[this] as Int
    set(value) {
        themeResField[this] = value
    }