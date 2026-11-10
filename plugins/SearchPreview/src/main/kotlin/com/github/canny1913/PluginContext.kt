package com.github.canny1913

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.AssetManager
import android.content.res.Resources
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import androidx.annotation.StyleRes
import com.aliucord.entities.Plugin
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Array as ReflectArray

class PluginContext(ctx: Context, @StyleRes themeId: Int, val plugin: Plugin) : ContextThemeWrapper(ctx, themeId) {
    private var mergedResources: Resources? = null

    init {
        if (plugin.resources != null) {

            val resources: Resources = this.plugin.resources!!
            val pluginAssets = apkAssetsGetter.invoke(resources.assets) as Array<*>
            val appAssets = apkAssetsGetter.invoke(ctx.resources.assets) as Array<*>
            val newAssets: List<*> = listOf(*pluginAssets, *appAssets)
            val newAssetManager = AssetManager::class.java.getDeclaredConstructor().newInstance()
            val newAssetsArray = ReflectArray.newInstance(apkAssetsClass, newAssets.size)
            for (i in 0..newAssets.size - 1) {
                ReflectArray.set(newAssetsArray, i, newAssets[i])
            }
            apkAssetsSetter(newAssetManager, newAssetsArray, true)

            @Suppress("DEPRECATION")
            this.mergedResources = Resources(newAssetManager, ctx.resources.displayMetrics, ctx.resources.configuration)
        }
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
}

@SuppressLint("PrivateApi")
val apkAssetsClass: Class<*> = Class.forName("android.content.res.ApkAssets")
val apkAssetsGetter: Method = AssetManager::class.java.getMethod("getApkAssets").apply { isAccessible = true }
val apkAssetsSetter: Method =
    AssetManager::class.java.getMethod("setApkAssets",
        ReflectArray.newInstance(apkAssetsClass, 0)::class.java,
        Boolean::class.java)
        .apply { isAccessible = true }

@SuppressLint("PrivateApi")
val themeResField: Field = ContextThemeWrapper::class.java.getDeclaredField("mThemeResource").apply { isAccessible = true }

var ContextThemeWrapper.themeResId: Int
    get() = themeResField[this] as Int
    set(value) {
        themeResField[this] = value
    }
