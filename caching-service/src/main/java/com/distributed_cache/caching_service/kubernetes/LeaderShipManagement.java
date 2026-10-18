package com.distributed_cache.caching_service.kubernetes;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoordinationV1Api;
import io.kubernetes.client.openapi.models.V1Lease;
import io.kubernetes.client.util.ClientBuilder;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Component
public class LeaderShipManagement {

    private static final String LEASE_NAME =
            "cache-shard-0-leader";

    private static final String NAMESPACE = "default";

    // Temporary identity for local testing.
    // Each deployed replica must have a unique identity.
    private static final String INSTANCE_ID =
            UUID.randomUUID().toString();

    private static final int LEASE_DURATION_SECONDS = 15;

    @Getter
    private volatile boolean leader = false;

    private CoordinationV1Api api;

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
                    INSTANCE_ID.equals(holder);

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
                        .setHolderIdentity(INSTANCE_ID);

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

                if (!leader) {
                    System.out.println(
                            "Leadership acquired by " + INSTANCE_ID);
                } else {
                    System.out.println("Lease renewed.");
                }

                leader = true;

            } else {

                if (leader) {
                    System.out.println(
                            "Leadership lost. Stopping leader-only work.");
                }

                leader = false;

                System.out.println(
                        "Follower. Current holder: " + holder);
            }

        } catch (ApiException e) {

            // We could not confirm or renew leadership.
            // Fail closed rather than continuing leader-only work.
            leader = false;

            if (e.getCode() == 409) {
                System.out.println(
                        "Lease update conflict; will retry.");
            } else {
                System.err.println(
                        "Kubernetes API error: " + e.getCode());
            }

        } catch (Exception e) {
            leader = false;

            System.err.println(
                    "Election check failed: " + e.getMessage());
        }
    }

}
