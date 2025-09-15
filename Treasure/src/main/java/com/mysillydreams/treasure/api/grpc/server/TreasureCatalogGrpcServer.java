package com.mysillydreams.treasure.api.grpc.server;

import com.google.protobuf.Empty;
import com.mysillydreams.treasure.api.rest.dto.request.SearchRequest;
import com.mysillydreams.treasure.domain.model.*;
import com.mysillydreams.treasure.domain.repository.*;
import com.mysillydreams.treasure.domain.service.PlanService;
import com.mysillydreams.treasure.domain.service.SearchService;
import com.mysillydreams.treasure.grpc.catalog.v1.*;
import com.mysillydreams.treasure.grpc.common.v1.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@GrpcService
@RequiredArgsConstructor
public class TreasureCatalogGrpcServer extends TreasureCatalogServiceGrpc.TreasureCatalogServiceImplBase {

    private final CategoryRepository categoryRepo;
    private final SubcategoryRepository subRepo;
    private final AgeBandRepository ageBandRepo;
    private final PlanPriceRepository priceRepo;
    private final SearchService searchService;
    private final PlanService planService;

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    // @Override
    public void ListCategories(Empty request, StreamObserver<ListCategoriesResponse> responseObserver) {
        var items = categoryRepo.findAll().stream().map(c ->
                ListCategoriesResponse.Category.newBuilder()
                        .setId(c.getId().toString())
                        .setName(c.getName())
                        .setDescription(c.getDescription()==null?"":c.getDescription())
                        .setAudience(c.getAudience().name())
                        .build()
        ).collect(Collectors.toList());
        responseObserver.onNext(ListCategoriesResponse.newBuilder().addAllCategories(items).build());
        responseObserver.onCompleted();
    }

    // @Override
    public void ListSubcategories(ListSubcategoriesRequest req, StreamObserver<ListSubcategoriesResponse> rsp) {
        var subs = subRepo.findByCategoryIdAndActiveTrue(java.util.UUID.fromString(req.getCategoryId()));
        var out = subs.stream().map(s -> {
            var b = ListSubcategoriesResponse.Subcategory.newBuilder()
                    .setId(s.getId().toString()).setName(s.getName())
                    .setDescription(s.getDescription()==null?"":s.getDescription());
            s.getAllowedAgeBands().forEach(ab -> b.addAgeBands(
                    com.mysillydreams.treasure.grpc.common.v1.AgeBand.newBuilder().setLabel(ab.getLabel()).setMinAge(ab.getMinAge()).setMaxAge(ab.getMaxAge()).build()));
            return b.build();
        }).toList();
        rsp.onNext(ListSubcategoriesResponse.newBuilder().addAllSubcategories(out).build());
        rsp.onCompleted();
    }

    // @Override
    public void ListPlans(ListPlansRequest req, StreamObserver<ListPlansResponse> rsp) {
        var pageable = PageRequest.of(
                req.getPage().getPage() == 0 ? 0 : req.getPage().getPage(),
                req.getPage().getSize() == 0 ? 12 : req.getPage().getSize(),
                Sort.by("startAt").ascending());

        var sr = new SearchRequest(
                parseUUID(req.getSubcategoryId()), mapDifficulty(req.getDifficulty()),
                req.getLevel()==0?null:req.getLevel(),
                parseTime(req.getDateFrom()), parseTime(req.getDateTo()),
                mapTimeWindow(req.getTimeWindow()),
                parseDecimal(req.getPriceMin()), parseDecimal(req.getPriceMax()),
                blankToNull(req.getCity()),
                req.getWithinKm()==0?null:req.getWithinKm(),
                req.getHasSlots()?Boolean.TRUE:null,
                req.getAge()==0?null:req.getAge()
        );

        var page = searchService.search(sr, pageable, sr.age()==null?Optional.empty():Optional.of(sr.age()));
        List<com.mysillydreams.treasure.grpc.common.v1.PlanSummary> items = page.getContent().stream().map(p -> {
            BigDecimal priceFrom = priceRepo.findMinBaseAmount(p.getId());
            String dr = p.getDifficulties().isEmpty() ? "N/A" :
                    p.getDifficulties().stream()
                            .map(d -> d.getDifficulty().name()+" L"+d.getLevelNumber())
                            .sorted().findFirst().orElse("N/A");
            boolean hasFinite = p.getMaxParticipants()!=null;
            int availableView = p.getSlots().isEmpty()?0:p.getSlots().get(0).getAvailableView();

            return com.mysillydreams.treasure.grpc.common.v1.PlanSummary.newBuilder()
                    .setId(p.getId().toString())
                    .setTitle(p.getTitle())
                    .setSubcategoryName(p.getSubcategory().getName())
                    .setCity(nvl(p.getCity()))
                    .setIsVirtual(p.isVirtual())
                    .setTimeWindow(mapTimeWindowToGrpc(p.getTimeWindowType()))
                    .setStartAt(p.getStartAt()==null?"":p.getStartAt().format(ISO))
                    .setEndAt(p.getEndAt()==null?"":p.getEndAt().format(ISO))
                    .setDifficultyRange(dr)
                    .setPriceFrom(com.mysillydreams.treasure.grpc.common.v1.Money.newBuilder().setCurrency("INR").setAmount(priceFrom==null?"0":priceFrom.toPlainString()))
                    .setHasFiniteSlots(hasFinite)
                    .setAvailableView(availableView)
                    .build();
        }).toList();

        rsp.onNext(ListPlansResponse.newBuilder()
                .addAllItems(items)
                .setPage(PageInfo.newBuilder()
                        .setTotalElements(page.getTotalElements())
                        .setTotalPages(page.getTotalPages())
                        .setPage(pageable.getPageNumber())
                        .setSize(pageable.getPageSize()))
                .build());
        rsp.onCompleted();
    }

    // @Override
    public void GetPlan(GetPlanRequest req, StreamObserver<GetPlanResponse> rsp) {
        try {
            var p = planService.getPlanDetail(java.util.UUID.fromString(req.getPlanId()));
            var priceFrom = priceRepo.findMinBaseAmount(p.getId());

            PlanDetail.Builder b = PlanDetail.newBuilder()
                    .setId(p.getId().toString())
                    .setTitle(p.getTitle())
                    .setSummary(nvl(p.getSummary()))
                    .setVenueText(nvl(p.getVenueText()))
                    .setCity(nvl(p.getCity()))
                    .setCountry(nvl(p.getCountry()))
                    .setIsVirtual(p.isVirtual())
                    .setTimeWindow(mapTimeWindowToGrpc(p.getTimeWindowType()))
                    .setStartAt(p.getStartAt()==null?"":p.getStartAt().format(ISO))
                    .setEndAt(p.getEndAt()==null?"":p.getEndAt().format(ISO))
                    .setPricingPreview(PricingPreview.newBuilder()
                            .setBase(Money.newBuilder().setCurrency("INR").setAmount(priceFrom==null?"0":priceFrom.toPlainString()))
                            .setTotal(Money.newBuilder().setCurrency("INR").setAmount(priceFrom==null?"0":priceFrom.toPlainString()))
                            .build());

            p.getRules().forEach(r -> b.addRules(PlanDetail.Rule.newBuilder().setText(r.getRuleText()).setOrder(r.getDisplayOrder())));
            p.getTasks().forEach(t -> b.addTasks(PlanDetail.Task.newBuilder()
                    .setId(t.getId().toString()).setTitle(t.getTitle()).setDetails(nvl(t.getDetails())).setCrucial(t.isCrucial())));
            if (!p.getSlots().isEmpty()) {
                var s = p.getSlots().get(0);
                b.setSlot(PlanDetail.Slot.newBuilder()
                        .setCapacity(s.getCapacityNullMeansOpen()==null?-1:s.getCapacityNullMeansOpen())
                        .setReserved(s.getReserved())
                        .setAvailableView(s.getAvailableView()));
            }

            rsp.onNext(GetPlanResponse.newBuilder().setPlan(b.build()).build());
            rsp.onCompleted();

        } catch (Exception e) {
            rsp.onError(Status.NOT_FOUND.withDescription("Plan not found").asRuntimeException());
        }
    }

    private static java.util.UUID parseUUID(String s){ return (s==null||s.isBlank())?null:java.util.UUID.fromString(s); }
    private static java.time.OffsetDateTime parseTime(String s){ return (s==null||s.isBlank())?null:java.time.OffsetDateTime.parse(s); }
    private static BigDecimal parseDecimal(String s){ return (s==null||s.isBlank())?null:new BigDecimal(s); }
    private static String nvl(String s){ return s==null?"":s; }
    private static String blankToNull(String s){ return (s==null||s.isBlank())?null:s; }

    private static com.mysillydreams.treasure.domain.model.Difficulty mapDifficulty(com.mysillydreams.treasure.grpc.common.v1.Difficulty d) {
        return d==com.mysillydreams.treasure.grpc.common.v1.Difficulty.DIFF_UNSPECIFIED ? null :
               switch (d) {
                   case BEGINNER -> com.mysillydreams.treasure.domain.model.Difficulty.BEGINNER;
                   case INTERMEDIATE -> com.mysillydreams.treasure.domain.model.Difficulty.INTERMEDIATE;
                   case ADVANCED -> com.mysillydreams.treasure.domain.model.Difficulty.ADVANCED;
                   default -> null;
               };
    }
    private static com.mysillydreams.treasure.domain.model.TimeWindowType mapTimeWindow(com.mysillydreams.treasure.grpc.common.v1.TimeWindowType t) {
        return t==com.mysillydreams.treasure.grpc.common.v1.TimeWindowType.TWT_UNSPECIFIED ? null :
               switch (t) {
                   case DAY -> com.mysillydreams.treasure.domain.model.TimeWindowType.DAY;
                   case NIGHT -> com.mysillydreams.treasure.domain.model.TimeWindowType.NIGHT;
                   case FULL_DAY -> com.mysillydreams.treasure.domain.model.TimeWindowType.FULL_DAY;
                   case MULTI_DAY -> com.mysillydreams.treasure.domain.model.TimeWindowType.MULTI_DAY;
                   default -> null;
               };
    }

    private static com.mysillydreams.treasure.grpc.common.v1.TimeWindowType mapTimeWindowToGrpc(com.mysillydreams.treasure.domain.model.TimeWindowType t) {
        return switch (t) {
            case DAY -> com.mysillydreams.treasure.grpc.common.v1.TimeWindowType.DAY;
            case NIGHT -> com.mysillydreams.treasure.grpc.common.v1.TimeWindowType.NIGHT;
            case FULL_DAY -> com.mysillydreams.treasure.grpc.common.v1.TimeWindowType.FULL_DAY;
            case MULTI_DAY -> com.mysillydreams.treasure.grpc.common.v1.TimeWindowType.MULTI_DAY;
        };
    }

}

