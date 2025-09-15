package com.mysillydreams.treasure.api.grpc.server;

import com.google.protobuf.Empty;
import com.mysillydreams.treasure.domain.model.*;
import com.mysillydreams.treasure.domain.model.Difficulty;
import com.mysillydreams.treasure.domain.service.EnrollmentService;
import com.mysillydreams.treasure.domain.service.TaskProgressService;
import com.mysillydreams.treasure.domain.service.UserLevelService;
import com.mysillydreams.treasure.grpc.enrollment.v1.*;
import com.mysillydreams.treasure.grpc.common.v1.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
public class TreasureEnrollmentGrpcServer extends TreasureEnrollmentServiceGrpc.TreasureEnrollmentServiceImplBase {

    private final EnrollmentService enrollmentService;
    private final TaskProgressService taskProgressService;
    private final UserLevelService userLevelService;

    // @Override
    public void Enroll(EnrollRequest req, StreamObserver<EnrollResponse> rsp) {
        try {
            UUID planId = UUID.fromString(req.getPlanId());
            UUID userId = UUID.fromString(req.getUserId());

            // Convert protobuf enum to domain enum
            com.mysillydreams.treasure.domain.model.EnrollmentType enrollmentType = convertEnrollmentType(req.getEnrollmentType());

            // Extract team details if provided
            String teamName = req.getTeamName().isEmpty() ? null : req.getTeamName();
            Integer teamSize = req.getTeamSize() == 0 ? null : req.getTeamSize();

            Enrollment e = enrollmentService.enroll(planId, userId, enrollmentType, teamName, teamSize);

            rsp.onNext(EnrollResponse.newBuilder()
                    .setEnrollmentId(e.getId().toString())
                    .setMode(map(e.getMode()))
                    .setStatus(map(e.getStatus()))
                    .setPaymentStatus(map(e.getPaymentStatus()))
                    .setRegistrationId(e.getRegistrationId())
                    .setEnrollmentType(map(e.getEnrollmentType()))
                    .setTeamName(e.getTeamName() != null ? e.getTeamName() : "")
                    .setTeamSize(e.getTeamSize() != null ? e.getTeamSize() : 0)
                    .build());
            rsp.onCompleted();
        } catch (IllegalStateException ex) {
            rsp.onError(Status.FAILED_PRECONDITION.withDescription(ex.getMessage()).asRuntimeException());
        } catch (Exception ex) {
            rsp.onError(Status.INTERNAL.withDescription("enroll failed").asRuntimeException());
        }
    }

    // @Override
    public void Approve(ApproveRequest req, StreamObserver<EnrollResponse> rsp) {
        Enrollment e = enrollmentService.approve(UUID.fromString(req.getEnrollmentId()), UUID.fromString(req.getApprovedBy()));
        rsp.onNext(EnrollResponse.newBuilder()
                .setEnrollmentId(e.getId().toString())
                .setMode(map(e.getMode()))
                .setStatus(map(e.getStatus()))
                .setPaymentStatus(map(e.getPaymentStatus()))
                .build());
        rsp.onCompleted();
    }

    // @Override
    public void Reject(RejectRequest req, StreamObserver<Empty> rsp) {
        enrollmentService.cancel(UUID.fromString(req.getEnrollmentId()));
        rsp.onNext(Empty.getDefaultInstance());
        rsp.onCompleted();
    }

    // @Override
    public void Cancel(CancelRequest req, StreamObserver<Empty> rsp) {
        enrollmentService.cancel(UUID.fromString(req.getEnrollmentId()));
        rsp.onNext(Empty.getDefaultInstance());
        rsp.onCompleted();
    }

    // @Override
    public void CompleteTask(CompleteTaskRequest req, StreamObserver<CompleteTaskResponse> rsp) {
        var tp = taskProgressService.completeTask(UUID.fromString(req.getEnrollmentId()), UUID.fromString(req.getTaskId()));
        rsp.onNext(CompleteTaskResponse.newBuilder()
                .setTaskProgressId(tp.getId().toString())
                .setStatus(tp.getStatus().name())
                .build());
        rsp.onCompleted();
    }

    // @Override
    public void GetUserLevels(GetUserLevelsRequest req, StreamObserver<UserLevelSummary> rsp) {
        var m = userLevelService.getSummary(UUID.fromString(req.getUserId()));
        rsp.onNext(UserLevelSummary.newBuilder()
                .setBeginner(m.getOrDefault(Difficulty.BEGINNER, 0))
                .setIntermediate(m.getOrDefault(Difficulty.INTERMEDIATE, 0))
                .setAdvanced(m.getOrDefault(Difficulty.ADVANCED, 0))
                .build());
        rsp.onCompleted();
    }

    // @Override
    public void RecalculateProgress(GetUserLevelsRequest req, StreamObserver<Empty> rsp) {
        userLevelService.evaluateOnTaskCompletion(UUID.fromString(req.getUserId()));
        rsp.onNext(Empty.getDefaultInstance());
        rsp.onCompleted();
    }

    private static com.mysillydreams.treasure.grpc.common.v1.EnrollmentMode map(com.mysillydreams.treasure.domain.model.EnrollmentMode m) {
        return m==null?com.mysillydreams.treasure.grpc.common.v1.EnrollmentMode.MODE_UNSPECIFIED:
                m==com.mysillydreams.treasure.domain.model.EnrollmentMode.APPROVAL_REQUIRED ? com.mysillydreams.treasure.grpc.common.v1.EnrollmentMode.APPROVAL_REQUIRED : com.mysillydreams.treasure.grpc.common.v1.EnrollmentMode.PAY_TO_ENROLL;
    }
    private static com.mysillydreams.treasure.grpc.common.v1.EnrollmentStatus map(com.mysillydreams.treasure.domain.model.EnrollmentStatus s) {
        return switch (s) {
            case PENDING -> com.mysillydreams.treasure.grpc.common.v1.EnrollmentStatus.PENDING;
            case CONFIRMED -> com.mysillydreams.treasure.grpc.common.v1.EnrollmentStatus.CONFIRMED;
            case REJECTED -> com.mysillydreams.treasure.grpc.common.v1.EnrollmentStatus.REJECTED;
            case CANCELLED -> com.mysillydreams.treasure.grpc.common.v1.EnrollmentStatus.CANCELLED;
        };
    }
    private static com.mysillydreams.treasure.grpc.common.v1.PaymentStatus map(com.mysillydreams.treasure.domain.model.PaymentStatus s) {
        return switch (s) {
            case NONE -> com.mysillydreams.treasure.grpc.common.v1.PaymentStatus.NONE;
            case AWAITING -> com.mysillydreams.treasure.grpc.common.v1.PaymentStatus.AWAITING;
            case PAID -> com.mysillydreams.treasure.grpc.common.v1.PaymentStatus.PAID;
            case REFUNDED -> com.mysillydreams.treasure.grpc.common.v1.PaymentStatus.REFUNDED;
        };
    }

    private static com.mysillydreams.treasure.domain.model.EnrollmentType convertEnrollmentType(com.mysillydreams.treasure.grpc.common.v1.EnrollmentType grpcType) {
        return switch (grpcType) {
            case INDIVIDUAL -> com.mysillydreams.treasure.domain.model.EnrollmentType.INDIVIDUAL;
            case TEAM -> com.mysillydreams.treasure.domain.model.EnrollmentType.TEAM;
            case TYPE_UNSPECIFIED, UNRECOGNIZED -> com.mysillydreams.treasure.domain.model.EnrollmentType.INDIVIDUAL; // Default to individual
        };
    }

    private static com.mysillydreams.treasure.grpc.common.v1.EnrollmentType map(com.mysillydreams.treasure.domain.model.EnrollmentType type) {
        return switch (type) {
            case INDIVIDUAL -> com.mysillydreams.treasure.grpc.common.v1.EnrollmentType.INDIVIDUAL;
            case TEAM -> com.mysillydreams.treasure.grpc.common.v1.EnrollmentType.TEAM;
        };
    }
}
