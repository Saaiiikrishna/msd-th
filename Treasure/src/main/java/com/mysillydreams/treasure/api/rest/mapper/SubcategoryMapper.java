package com.mysillydreams.treasure.api.rest.mapper;

import com.mysillydreams.treasure.api.rest.dto.response.SubcategoryResponse;
import com.mysillydreams.treasure.domain.model.Subcategory;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SubcategoryMapper {
    SubcategoryResponse toResponse(Subcategory e);
    // custom method to bind age bands after lookup
}
