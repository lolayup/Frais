package com.khaled.frais.utils

import android.annotation.SuppressLint
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.khaled.frais.FraisApp.Companion.app
import com.khaled.frais.app.FraisData

object IconPack {
    @SuppressLint("DiscouragedApi")
    fun loadIcon(packageName: String): Bitmap? = runCatching {
        val resources = app.packageManager.getResourcesForApplication(FraisData.iconPack)
        getResourceName(resources, FraisData.iconPack, packageName)?.let {
            return BitmapFactory.decodeResource(
                resources, resources.getIdentifier(it, "drawable", FraisData.iconPack)
            )
        }
    }.getOrNull()

    @SuppressLint("DiscouragedApi")
    private fun getResourceName(
        resources: Resources, resPackage: String, componentName: String
    ): String? {
        val parser = resources.getXml(resources.getIdentifier("appfilter", "xml", resPackage))
        while (parser.eventType != XmlResourceParser.END_DOCUMENT) {
            runCatching {
                if (parser.eventType == XmlResourceParser.START_TAG && parser.getAttributeValue(0)
                        .contains(componentName)
                ) {
                    return parser.getAttributeValue(1)
                }
            }
            parser.next()
        }
        return null
    }
}
