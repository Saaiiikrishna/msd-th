package com.mysillydreams.treasure.cache;

import com.mysillydreams.treasure.api.rest.dto.request.SearchRequest;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.UUID;

public final class CacheKeys {
    public static String subcats(UUID categoryId) { return "catalog:subcats:" + categoryId; }
    public static String searchKey(SearchRequest req) {
        // Stable string for cache key
        return DigestUtils.sha256Hex(
                (""+req.subcategoryId()+req.difficulty()+req.level()+req.dateFrom()+req.dateTo()
                        +req.timeWindowType()+req.priceMin()+req.priceMax()+req.city()+req.withinKm()+req.hasSlots()+req.age())
        );
    }
    private CacheKeys() {}
}

