package com.rich_beluga.isexplorer

import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import com.google.android.material.R as MaterialR

enum class FileCategory(
    @DrawableRes val iconRes: Int,
    @AttrRes  val containerColorAttr: Int,
    @AttrRes  val onContainerColorAttr: Int
) {
    /* images */
    IMAGE(
        R.drawable.ic_type_image,
        MaterialR.attr.colorTertiaryContainer,
        MaterialR.attr.colorOnTertiaryContainer
    ),
    
    /* videos */
    VIDEO(
        R.drawable.ic_type_video,
        MaterialR.attr.colorErrorContainer,
        MaterialR.attr.colorOnErrorContainer
    ),
    
    /* audio */
    AUDIO(
        R.drawable.ic_type_audio,
        MaterialR.attr.colorSecondaryContainer,
        MaterialR.attr.colorOnSecondaryContainer
    ),
    
    /* pdf */
    PDF(
        R.drawable.ic_type_pdf,
        MaterialR.attr.colorErrorContainer,
        MaterialR.attr.colorOnErrorContainer
    ),
    
    /* .icrosoft office files (*.docx, *.pptx, etc) */
    DOCUMENT(
        R.drawable.ic_type_doc,
        MaterialR.attr.colorPrimaryContainer,
        MaterialR.attr.colorOnPrimaryContainer
    ),
    
    /* Code files */
    SHELL(
        R.drawable.ic_type_shell,
        MaterialR.attr.colorSecondaryContainer,
        MaterialR.attr.colorOnSecondaryContainer
    ),
    PYTHON(
        R.drawable.ic_type_python,
        MaterialR.attr.colorSecondaryContainer,
        MaterialR.attr.colorOnSecondaryContainer
    ),
    CLANG(
        R.drawable.ic_type_clang,
        MaterialR.attr.colorSecondaryContainer,
        MaterialR.attr.colorOnSecondaryContainer
    ),
    RUST(
        R.drawable.ic_type_rust,
        MaterialR.attr.colorSecondaryContainer,
        MaterialR.attr.colorOnSecondaryContainer
    ),
    GOLANG(
        R.drawable.ic_type_go,
        MaterialR.attr.colorSecondaryContainer,
        MaterialR.attr.colorOnSecondaryContainer
    ),
    KOTLIN(
        R.drawable.ic_type_kotlin,
        MaterialR.attr.colorSecondaryContainer,
        MaterialR.attr.colorOnSecondaryContainer
    ),
    JAVA(
        R.drawable.ic_type_java,
        MaterialR.attr.colorSecondaryContainer,
        MaterialR.attr.colorOnSecondaryContainer
    ),
    CPP(
        R.drawable.ic_type_cpp,
        MaterialR.attr.colorSecondaryContainer,
        MaterialR.attr.colorOnSecondaryContainer
    ),
    HTML(
        R.drawable.ic_type_html,
        MaterialR.attr.colorSecondaryContainer,
        MaterialR.attr.colorOnSecondaryContainer
    ),
    JAVASCRIPT(
        R.drawable.ic_type_javascript,
        MaterialR.attr.colorSecondaryContainer,
        MaterialR.attr.colorOnSecondaryContainer
    ),
    CSS(
        R.drawable.ic_type_css,
        MaterialR.attr.colorSecondaryContainer,
        MaterialR.attr.colorOnSecondaryContainer
    ),
    PHP(
        R.drawable.ic_type_php,
        MaterialR.attr.colorSecondaryContainer,
        MaterialR.attr.colorOnSecondaryContainer
    ),
    JSON(
        R.drawable.ic_type_json,
        MaterialR.attr.colorSecondaryContainer,
        MaterialR.attr.colorOnSecondaryContainer
    ),
    MARKDOWN(
        R.drawable.ic_type_markdown,
        MaterialR.attr.colorSecondaryContainer,
        MaterialR.attr.colorOnSecondaryContainer
    ),
    XML(
        R.drawable.ic_type_xml,
        MaterialR.attr.colorSecondaryContainer,
        MaterialR.attr.colorOnSecondaryContainer
    ),
    YAML(
        R.drawable.ic_type_yaml,
        MaterialR.attr.colorSecondaryContainer,
        MaterialR.attr.colorOnSecondaryContainer
    ),
    GROOVY(
        R.drawable.ic_type_groovy,
        MaterialR.attr.colorSecondaryContainer,
        MaterialR.attr.colorOnSecondaryContainer
    ),
    TYPESCRIPT(
        R.drawable.ic_type_typescript,
        MaterialR.attr.colorSecondaryContainer,
        MaterialR.attr.colorOnSecondaryContainer
    ),
    
    
    /* zip, rar, etc archives */
    ARCHIVE(
        R.drawable.ic_type_archive,
        MaterialR.attr.colorTertiaryContainer,
        MaterialR.attr.colorOnTertiaryContainer
    ),
    
    /* android application */
    APK(
        R.drawable.ic_type_apk,
        MaterialR.attr.colorPrimaryContainer,
        MaterialR.attr.colorOnPrimaryContainer
    ),
    
    /* text documents */
    TEXT(
        R.drawable.ic_type_text,
        MaterialR.attr.colorSurfaceVariant,
        MaterialR.attr.colorOnSurfaceVariant
    ),
    CONFIG(
        R.drawable.ic_type_config,
        MaterialR.attr.colorSurfaceVariant,
        MaterialR.attr.colorOnSurfaceVariant
    ),
    
    /* folders */
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
