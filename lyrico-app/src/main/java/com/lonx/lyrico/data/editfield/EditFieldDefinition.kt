package com.lonx.lyrico.data.editfield

import androidx.annotation.StringRes
import com.lonx.lyrico.data.model.metadata.MetadataFieldTarget

data class EditFieldDefinition(
    val code: String,
    val groupCode: String,
    @field:StringRes val titleRes: Int,
    val defaultVisible: Boolean = true,
    val order: Int,
    val scope: EditFieldScope = EditFieldScope.Both,
    val kind: EditFieldKind = EditFieldKind.Text,
    val simpleTextInput: Boolean = kind == EditFieldKind.Text || kind == EditFieldKind.Date ||
        kind == EditFieldKind.PersonList || kind == EditFieldKind.ReplayGain,
    val target: MetadataFieldTarget? = null,
    /** 是否为用户添加的自定义标签。 */
    val custom: Boolean = false,
)
