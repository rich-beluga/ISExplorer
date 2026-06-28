package com.rich_beluga.isexplorer

import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import com.google.android.material.R as MaterialR

enum class FileCategory(
    @DrawableRes val iconRes: Int,
    @AttrRes  val containerColorAttr: Int,
    @AttrRes  val onContainerColorAttr: Int
) {
    IMAGE(
        R.drawable.ic_type_image,
        MaterialR.attr.colorTertiaryContainer,
        MaterialR.attr.colorOnTertiaryContainer
    ),
    VIDEO(
        R.drawable.ic_type_video,
        MaterialR.attr.colorErrorContainer,
        MaterialR.attr.colorOnErrorContainer
    ),
    AUDIO(
        R.drawable.ic_type_audio,
        MaterialR.attr.colorSecondaryContainer,
        MaterialR.attr.colorOnSecondaryContainer
    ),

    PDF(
        R.drawable.ic_type_pdf,
        MaterialR.attr.colorErrorContainer,
        MaterialR.attr.colorOnErrorContainer
    ),
    DOCUMENT(
        R.drawable.ic_type_doc,
        MaterialR.attr.colorPrimaryContainer,
        MaterialR.attr.colorOnPrimaryContainer
    ),

    CODE(
        R.drawable.ic_type_code,
        MaterialR.attr.colorSecondaryContainer,
        MaterialR.attr.colorOnSecondaryContainer
    ),

    ARCHIVE(
        R.drawable.ic_type_archive,
        MaterialR.attr.colorTertiaryContainer,
        MaterialR.attr.colorOnTertiaryContainer
    ),

    APK(
        R.drawable.ic_type_apk,
        MaterialR.attr.colorPrimaryContainer,
        MaterialR.attr.colorOnPrimaryContainer
    ),

    TEXT(
        R.drawable.ic_type_text,
        MaterialR.attr.colorSurfaceVariant,
        MaterialR.attr.colorOnSurfaceVariant
    ),

    FOLDER(
        R.drawable.ic_folder,
        MaterialR.attr.colorPrimaryContainer,
        MaterialR.attr.colorOnPrimaryContainer
    ),
    PARENT_DIR(
        R.drawable.ic_back,
        MaterialR.attr.colorSurfaceVariant,
        MaterialR.attr.colorOnSurfaceVariant
    ),
    UNKNOWN(
        R.drawable.ic_type_text,
        MaterialR.attr.colorSurfaceVariant,
        MaterialR.attr.colorOnSurfaceVariant
    )
}
