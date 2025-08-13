package com.mysillydreams.treasure.api.rest.mapper;

import org.mapstruct.factory.Mappers;
public final class PlanMapperImplFactory {
    public static final PlanMapper INSTANCE = Mappers.getMapper(PlanMapper.class);
    private PlanMapperImplFactory() {}
}
