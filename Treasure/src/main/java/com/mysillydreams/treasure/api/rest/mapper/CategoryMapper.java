package com.mysillydreams.treasure.api.rest.mapper;

import com.mysillydreams.treasure.api.rest.dto.request.CreateCategoryRequest;
import com.mysillydreams.treasure.api.rest.dto.response.CategoryResponse;
import com.mysillydreams.treasure.domain.model.Category;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    CategoryResponse toResponse(Category category);
    Category toEntity(CreateCategoryRequest req);
}
