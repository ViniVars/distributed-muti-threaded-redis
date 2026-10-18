package com.distributed_cache.caching_service.service;

import com.distributed_cache.caching_service.commons.GlobalVariableClass;
import com.distributed_cache.caching_service.repository.FencingTokenRepository;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoordinationV1Api;
import io.kubernetes.client.openapi.models.V1Lease;
import io.kubernetes.client.util.ClientBuilder;

import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class LeadershipService {

    private static final String LEASE_NAME =
            "cache-shard-0-leader";

    private static final String NAMESPACE = "default";

    // Temporary identity for local testing.
    // Each deployed replica must have a unique identity.



    private static final int LEASE_DURATION_SECONDS = 15;


    private CoordinationV1Api api;
    private final GlobalVariableClass globalVariableClass;
    private final FencingTokenRepository fencingTokenRepository;
    private final SynchronizationService synchronizationService;

    public LeadershipService(GlobalVariableClass globalVariableClass, FencingTokenRepository fencingTokenRepository, SynchronizationService synchronizationService) {
        this.globalVariableClass = globalVariableClass;
        this.fencingTokenRepository = fencingTokenRepository;
        this.synchronizationService = synchronizationService;
    }

    @PostConstruct
    public void initialize() throws Exception {
        ApiClient client = ClientBuilder.defaultClient();
        api = new CoordinationV1Api(client);

        System.out.println(
                "Leader-election scheduler initialized.");
    }

    // Runs after the previous invocation finishes,
    // then waits 2 seconds before running again.
    @Scheduled(fixedDelay = 2000)
    public void electionTick() {

        if (!globalVariableClass.isSnapped()){
            return;
        }

        try {
            V1Lease lease = api
                    .readNamespacedLease(LEASE_NAME, NAMESPACE)
                    .execute();

            OffsetDateTime now =
                    OffsetDateTime.now(ZoneOffset.UTC);

            String holder =
                    lease.getSpec().getHolderIdentity();

            boolean unclaimed =
                    holder == null || holder.isBlank();

            boolean alreadyLeader =
                    globalVariableClass.getInstanceID().equals(holder);

            OffsetDateTime renewTime =
                    lease.getSpec().getRenewTime();

            Integer duration =
                    lease.getSpec().getLeaseDurationSeconds();

            // Only take over when expiry can be established
            // from the current Lease timestamps.
            boolean expired =
                    !unclaimed
                            && !alreadyLeader
                            && renewTime != null
                            && duration != null
                            && !now.isBefore(
                            renewTime.plusSeconds(duration));

            if (alreadyLeader || unclaimed || expired) {

                lease.getSpec()
                        .setHolderIdentity(globalVariableClass.getInstanceID());

                lease.getSpec()
                        .setLeaseDurationSeconds(
                                LEASE_DURATION_SECONDS);

                lease.getSpec().setRenewTime(now);

                if (unclaimed) {
                    lease.getSpec().setAcquireTime(now);
                }

                if (expired) {
                    Integer transitions =
                            lease.getSpec().getLeaseTransitions();

                    lease.getSpec().setLeaseTransitions(
                            transitions == null
                                    ? 1 : transitions + 1);

                    lease.getSpec().setAcquireTime(now);
                }

                // The resourceVersion from the read is retained.
                // Kubernetes rejects conflicting updates with HTTP 409.
                api.replaceNamespacedLease(
                        LEASE_NAME, NAMESPACE, lease
                ).execute();

                if (!globalVariableClass.isLeader) {

                    System.out.println(
                            "Leadership acquired by " + globalVariableClass.getInstanceID());

                    globalVariableClass.setFencingToken(fencingTokenRepository.incrementToken(0L));
                    synchronizationService.outboxSync();

                    globalVariableClass.setLeader(true);
                    globalVariableClass.setReady(true);

                }
//                else {
//                    System.out.println("Lease renewed.");
//                }

//                System.out.println(globalVariableClass.getFencingToken());

            } else {

                if (globalVariableClass.isLeader) {
                    System.out.println(
                            "Leadership lost. Stopping Leader-only work.");
                }

                globalVariableClass.setLeader(false);
                globalVariableClass.setReady(false);

                System.out.println(
                        "Follower. Current holder: " + holder);
            }

        } catch (ApiException e) {

            // We could not confirm or renew Leadership.
            // Fail closed rather than continuing Leader-only work.
            globalVariableClass.setLeader(false);
            globalVariableClass.setReady(false);

            if (e.getCode() == 409) {
                System.out.println(
                        "Lease update conflict; will retry.");
            } else {
                System.err.println(
                        "Kubernetes API error: " + e.getCode());
            }

        } catch (Exception e) {

            globalVariableClass.setLeader(false);
            globalVariableClass.setReady(false);

            System.err.println(
                    "Election check failed: " + e.getMessage());
        }
    }

}
