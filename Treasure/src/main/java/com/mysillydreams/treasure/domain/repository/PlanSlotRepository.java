package com.mysillydreams.treasure.domain.repository;

import com.mysillydreams.treasure.domain.model.PlanSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PlanSlotRepository extends JpaRepository<PlanSlot, UUID> {
    Optional<PlanSlot> findByPlanId(UUID planId);

    // Finite slots: optimistic-atomic reservation
    @Modifying
    @Query("""
    update PlanSlot s
       set s.reserved = s.reserved + :qty
     where s.plan.id = :planId
       and s.capacityNullMeansOpen is not null
       and (s.reserved + :qty) <= s.capacityNullMeansOpen
  """)
    int tryReserve(@Param("planId") UUID planId, @Param("qty") int qty);

    @Modifying
    @Query("""
    update PlanSlot s
       set s.reserved = greatest(s.reserved - :qty, 0)
     where s.plan.id = :planId
       and s.capacityNullMeansOpen is not null
  """)
    int release(@Param("planId") UUID planId, @Param("qty") int qty);
}
