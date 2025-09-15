package com.mysillydreams.treasure.domain.repository;

import com.mysillydreams.treasure.domain.model.PlanPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface PlanPriceRepository extends JpaRepository<PlanPrice, UUID> {
    @Query(value = """
     select * from plan_price p
     where p.plan_id=:planId and p.currency=:currency
       and (p.validity is null or p.validity @> CURRENT_TIMESTAMP::timestamp)
     order by p.base_amount asc
  """, nativeQuery = true)
    List<PlanPrice> findActiveByPlanAndCurrency(@Param("planId") UUID planId, @Param("currency") String currency);

    @Query("select min(p.baseAmount) from PlanPrice p where p.plan.id=:planId")
    BigDecimal findMinBaseAmount(@Param("planId") UUID planId);
}
