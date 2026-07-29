package com.awan.app.core.data.category

import com.awan.app.core.model.Category
import com.awan.app.core.network.dto.category.CategoryDto

internal fun CategoryDto.toModel(): Category = Category(id = id, name = name)
