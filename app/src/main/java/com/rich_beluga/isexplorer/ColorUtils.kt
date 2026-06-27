package com.rich_beluga.isexplorer

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue

fun resolveAttrColor(context: Context, attrResId: Int): Int {
    val tv = TypedValue()
    context.theme.resolveAttribute(attrResId, tv, true)
    return tv.data
}

val Int.dp: Int get() = (this * Resources.getSystem().displayMetrics.density + 0.5f).toInt()
val Float.dp: Float get() = this * Resources.getSystem().displayMetrics.density
