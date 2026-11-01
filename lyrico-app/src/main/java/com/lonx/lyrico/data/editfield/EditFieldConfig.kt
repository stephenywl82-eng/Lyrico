package com.lonx.lyrico.data.editfield

/**
 * 编辑字段配置：字段顺序与显隐。
 * [order] 为内置字段与自定义标签的混排顺序；[overrides] 只记录用户显式改过的显隐，缺省回落到
 * `defaultVisible`；[customTags] 为已加入列表的自定义标签键。
 */
data class EditFieldConfig(
    val order: List<String> = emptyList(),
    val overrides: Map<String, Boolean> = emptyMap(),
    val customTags: List<String> = emptyList(),
    val componentOverrides: Map<String, Boolean> = emptyMap(),
) {

    val customFields: List<EditFieldDefinition> = customTags.map { key ->
        EditFieldDefinition(
            code = EditFieldRegistry.customTagCode(key),
            groupCode = EditFieldRegistry.GROUP_CUSTOM_TAGS,
            titleRes = 0,
            order = Int.MAX_VALUE,
            scope = EditFieldScope.Both,
            kind = EditFieldKind.Custom,
            target = null,
            custom = true,
        )
    }

    val allFields: List<EditFieldDefinition> = run {
        val byCode = (EditFieldRegistry.fields + customFields).associateBy { it.code }
        val ordered = order.mapNotNull { byCode[it] }
        val missing = byCode.values.filter { it.code !in order }
            .sortedBy { if (it.custom) Int.MAX_VALUE else it.order }
        (ordered + missing).distinctBy { it.code }
    }

    fun orderedCodes(): List<String> = allFields.map { it.code }

    fun isEnabled(code: String): Boolean {
        overrides[code]?.let { return it }
        EditFieldRegistry.fieldMap[code]?.let { return it.defaultVisible }
        // 自定义标签默认启用：用户是主动把它加进列表的。
        return customFields.any { it.code == code }
    }

    fun isEnabled(field: EditFieldDefinition): Boolean = isEnabled(field.code)

    fun isComponentEnabled(key: String): Boolean = componentOverrides[key] ?: true

    fun isEffectivelyEnabled(field: EditFieldDefinition): Boolean =
        (field.componentKey?.let(::isComponentEnabled) ?: true) && isEnabled(field)

    fun withComponentEnabled(key: String, enabled: Boolean): EditFieldConfig {
        if (allFields.none { it.componentKey == key }) return this
        return copy(componentOverrides = componentOverrides + (key to enabled))
    }

    fun isVisibleInScene(field: EditFieldDefinition, scene: EditFieldScene): Boolean =
        field.scope.supports(scene) && isEffectivelyEnabled(field)

    fun visibleFieldsForScene(scene: EditFieldScene): List<EditFieldDefinition> =
        allFields.toEditFieldBlocks().flatMap { block ->
            block.fields.filter { isVisibleInScene(it, scene) }
        }

    fun visibleFieldCodesForScene(scene: EditFieldScene): Set<String> =
        visibleFieldsForScene(scene).map { it.code }.toSet()

    fun visibleCustomTagKeys(scene: EditFieldScene): List<String> =
        visibleFieldsForScene(scene)
            .filter { it.custom }
            .mapNotNull { EditFieldRegistry.customTagKeyOf(it.code) }

    fun matchTargets(): List<com.lonx.lyrico.data.model.metadata.MetadataFieldTarget> =
        allFields.toEditFieldBlocks().flatMap { it.fields }.filter { isEffectivelyEnabled(it) }.mapNotNull { it.target }

    fun fieldItems(): List<EditFieldListItem> =
        allFields.map { field -> EditFieldListItem(field, isEnabled(field)) }

    fun withEnabled(code: String, enabled: Boolean): EditFieldConfig =
        copy(overrides = overrides + (code to enabled))

    fun withOrder(nextOrder: List<String>): EditFieldConfig {
        val known = allFields.map { it.code }.toSet()
        val normalized = nextOrder.filter { it in known }.distinct() +
            allFields.map { it.code }.filterNot { it in nextOrder }
        return copy(order = normalized.distinct())
    }

    fun withBlockOrder(keys: List<String>): EditFieldConfig {
        val blocks = allFields.toEditFieldBlocks().associateBy { it.key }
        val orderedKeys = (keys.filter { it in blocks } + blocks.keys).distinct()
        return withOrder(orderedKeys.flatMap { blocks.getValue(it).fields }.map { it.code })
    }

    fun withComponentOrder(key: String, codes: List<String>): EditFieldConfig {
        val blocks = allFields.toEditFieldBlocks()
        val component = blocks.firstOrNull { it.key == key && it.isComposite } ?: return this
        val members = component.fields.map { it.code }
        val orderedMembers = (codes.filter { it in members } + members).distinct()
        return withOrder(blocks.flatMap { block ->
            if (block.key == key) orderedMembers else block.fields.map { it.code }
        })
    }

    fun withAddedCustomTag(key: String): EditFieldConfig {
        if (key in customTags) return this
        val code = EditFieldRegistry.customTagCode(key)
        return copy(
            customTags = customTags + key,
            order = orderedCodes() + code,
            overrides = overrides + (code to true),
        )
    }

    fun withRemovedCustomTag(key: String): EditFieldConfig {
        val code = EditFieldRegistry.customTagCode(key)
        return copy(
            customTags = customTags - key,
            order = orderedCodes().filterNot { it == code },
            overrides = overrides - code,
        )
    }
}

/** 「编辑字段」页里的一行。 */
data class EditFieldListItem(
    val field: EditFieldDefinition,
    val enabled: Boolean,
)
