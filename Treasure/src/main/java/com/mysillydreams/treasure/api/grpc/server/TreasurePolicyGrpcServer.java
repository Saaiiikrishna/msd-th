package com.mysillydreams.treasure.api.grpc.server;

import com.google.protobuf.Empty;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.mysillydreams.treasure.domain.model.ProgressionPolicy;
import com.mysillydreams.treasure.domain.repository.ProgressionPolicyRepository;
import com.mysillydreams.treasure.grpc.policy.v1.*;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@GrpcService
@RequiredArgsConstructor
public class TreasurePolicyGrpcServer extends TreasurePolicyServiceGrpc.TreasurePolicyServiceImplBase {

    private final ProgressionPolicyRepository repo;

    // @Override
    public void GetPolicies(GetPoliciesRequest req, StreamObserver<GetPoliciesResponse> rsp) {
        var list = switch (req.getScope()) {
            case "GLOBAL" -> repo.findActiveGlobal();
            case "COHORT" -> repo.findActiveByCohort(req.getScopeRef());
            case "USER"   -> repo.findActiveByUser(req.getScopeRef());
            default       -> repo.findActiveGlobal();
        };
        var out = list.stream().map(this::toProto).collect(Collectors.toList());
        rsp.onNext(GetPoliciesResponse.newBuilder().addAllItems(out).build());
        rsp.onCompleted();
    }

    // @Override
    public void SetPolicy(SetPolicyRequest req, StreamObserver<Policy> rsp) {
        ProgressionPolicy p;
        if (req.getId()!=null && !req.getId().isBlank()) {
            p = repo.findById(UUID.fromString(req.getId())).orElse(new ProgressionPolicy());
            p.setId(p.getId());
        } else {
            p = new ProgressionPolicy();
        }
        p.setName("policy-"+req.getScope().toLowerCase());
        p.setScope(req.getScope());
        p.setScopeRef(emptyToNull(req.getScopeRef()));
        p.setPolicyJson(structToMap(req.getPolicyJson()));
        p.setActive(req.getActive());
        p = repo.save(p);
        rsp.onNext(toProto(p));
        rsp.onCompleted();
    }

    // @Override
    public void ApplyInviteOverride(ApplyInviteOverrideRequest req, StreamObserver<ApplyInviteOverrideResponse> rsp) {
        // Stub: persist a user-scoped policy with caps (admin or inviter privilege checks later)
        var json = Map.<String,Object>of(
                "invite_override", true,
                "beginner.min_levels", req.getBeginnerLevelCap(),
                "intermediate.min_levels", req.getIntermediateLevelCap()
        );
        var p = ProgressionPolicy.builder()
                .name("invite-override")
                .scope("USER").scopeRef(req.getInviteeUserId())
                .policyJson(json)
                .active(true).build();
        repo.save(p);
        rsp.onNext(ApplyInviteOverrideResponse.newBuilder().setApplied(true).setMessage("Override applied").build());
        rsp.onCompleted();
    }

    private Policy toProto(ProgressionPolicy p) {
        return Policy.newBuilder()
                .setId(p.getId().toString())
                .setScope(p.getScope())
                .setScopeRef(p.getScopeRef()==null?"":p.getScopeRef())
                .setPolicyJson(mapToStruct(p.getPolicyJson()))
                .setActive(p.isActive())
                .build();
    }

    private static Struct mapToStruct(Map<String,Object> m) {
        Struct.Builder b = Struct.newBuilder();
        m.forEach((k,v) -> b.putFields(k, toValue(v)));
        return b.build();
    }
    private static Value toValue(Object v) {
        if (v == null) return Value.newBuilder().setNullValueValue(0).build();
        if (v instanceof Number) return Value.newBuilder().setNumberValue(((Number)v).doubleValue()).build();
        if (v instanceof Boolean) return Value.newBuilder().setBoolValue((Boolean)v).build();
        if (v instanceof Map) return Value.newBuilder().setStructValue(mapToStruct((Map<String,Object>) v)).build();
        return Value.newBuilder().setStringValue(String.valueOf(v)).build();
    }
    private static Map<String,Object> structToMap(Struct s) {
        return s.getFieldsMap().entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey, e -> fromValue(e.getValue())
        ));
    }
    private static Object fromValue(Value v) {
        return switch (v.getKindCase()) {
            case NULL_VALUE -> null;
            case NUMBER_VALUE -> v.getNumberValue();
            case BOOL_VALUE -> v.getBoolValue();
            case STRING_VALUE -> v.getStringValue();
            case STRUCT_VALUE -> structToMap(v.getStructValue());
            case LIST_VALUE -> v.getListValue().getValuesList().stream().map(TreasurePolicyGrpcServer::fromValue).toList();
            case KIND_NOT_SET -> null;
        };
    }
    private static String emptyToNull(String s){ return (s==null||s.isBlank())?null:s; }
}
