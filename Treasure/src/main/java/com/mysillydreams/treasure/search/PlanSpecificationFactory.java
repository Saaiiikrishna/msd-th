package com.mysillydreams.treasure.search;

import com.mysillydreams.treasure.api.rest.dto.request.SearchRequest;
import com.mysillydreams.treasure.domain.model.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
public class PlanSpecificationFactory {

    public Specification<Plan> build(SearchRequest req, Optional<GeoFenceRule> fence, Optional<Integer> userAge) {
        return Specification.where(bySubcategory(req.subcategoryId()))
                .and(byDifficulty(req.difficulty(), req.level()))
                .and(byDateRange(req.dateFrom(), req.dateTo()))
                .and(byTimeWindow(req.timeWindowType()))
                .and(byPrice(req.priceMin(), req.priceMax()))
                .and(byCityOrRadius(req.city(), req.withinKm()))
                .and(applyGeoFence(fence))
                .and(applyAge(userAge));
    }

    private Specification<Plan> bySubcategory(UUID subId) {
        return (root, q, cb) -> subId == null ? null : cb.equal(root.get("subcategory").get("id"), subId);
    }

    private Specification<Plan> byDifficulty(Difficulty diff, Integer level) {
        return (root, q, cb) -> {
            if (diff == null && level == null) return null;
            Join<Plan, PlanDifficulty> pd = root.join("difficulties", JoinType.LEFT);
            Predicate p = cb.conjunction();
            if (diff != null) p = cb.and(p, cb.equal(pd.get("difficulty"), diff));
            if (level != null) p = cb.and(p, cb.greaterThanOrEqualTo(pd.get("levelNumber"), level));
            q.distinct(true);
            return p;
        };
    }

    private Specification<Plan> byDateRange(OffsetDateTime from, OffsetDateTime to) {
        return (root, q, cb) -> {
            if (from == null && to == null) return null;
            Predicate p = cb.conjunction();
            if (from != null) p = cb.and(p, cb.greaterThanOrEqualTo(root.get("startAt"), from));
            if (to   != null) p = cb.and(p, cb.lessThanOrEqualTo(root.get("endAt"), to));
            return p;
        };
    }

    private Specification<Plan> byTimeWindow(TimeWindowType t) {
        return (r,q,cb) -> t==null ? null : cb.equal(r.get("timeWindowType"), t);
    }

    private Specification<Plan> byPrice(BigDecimal min, BigDecimal max) {
        // Uses correlated subquery on PlanPrice if needed; keep null here for v1 (min/max applied post-read if currency-specific)
        return (r,q,cb) -> null;
    }

    private Specification<Plan> byCityOrRadius(String city, Integer withinKm) {
        return (root, q, cb) -> {
            if (city != null && !city.isBlank()) return cb.equal(root.get("city"), city);
            // withinKm over geo_point (PostGIS) is typically done via native query; defer for v1
            return null;
        };
    }

    private Specification<Plan> applyGeoFence(Optional<GeoFenceRule> fence) {
        return (r,q,cb) -> fence.filter(GeoFenceRule::isEnabled)
                .map(f -> switch (f.getScope()) {
                    case "CITY"    -> r.get("city").in(f.getValues());
                    case "COUNTRY" -> r.get("country").in(f.getValues());
                    default        -> null;
                })
                .orElse(null);
    }

    private Specification<Plan> applyAge(Optional<Integer> age) {
        return (root, q, cb) -> {
            if (age.isEmpty()) return null;
            // ensure age is permitted by Subcategory allowedAgeBands
            Join<Plan, Subcategory> sub = root.join("subcategory");
            Join<Subcategory, AgeBand> band = sub.join("allowedAgeBands", JoinType.LEFT);
            Predicate minOk = cb.lessThanOrEqualTo(band.get("minAge"), age.get());
            Predicate maxOk = cb.greaterThanOrEqualTo(band.get("maxAge"), age.get());
            q.distinct(true);
            return cb.and(minOk, maxOk);
        };
    }
}

