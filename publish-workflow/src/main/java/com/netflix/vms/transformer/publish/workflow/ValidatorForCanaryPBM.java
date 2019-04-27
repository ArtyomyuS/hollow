package com.netflix.vms.transformer.publish.workflow;

import static com.netflix.vms.transformer.common.io.TransformerLogTag.PlaybackMonkey;

import com.netflix.hollow.api.producer.HollowProducer;
import com.netflix.hollow.api.producer.validation.ValidationResult;
import com.netflix.hollow.api.producer.validation.ValidatorListener;
import com.netflix.hollow.core.read.engine.HollowReadStateEngine;
import com.netflix.vms.transformer.publish.workflow.job.CanaryAnnounceJob;
import com.netflix.vms.transformer.publish.workflow.job.CanaryRollbackJob;
import com.netflix.vms.transformer.publish.workflow.job.impl.CassandraCanaryValidationJob;
import com.netflix.vms.transformer.publish.workflow.job.impl.DefaultHollowPublishJobCreator;
import com.netflix.vms.transformer.publish.workflow.job.impl.HollowBlobAfterCanaryAnnounceJob;
import com.netflix.vms.transformer.publish.workflow.job.impl.HollowBlobBeforeCanaryAnnounceJob;
import java.util.Collections;
import java.util.function.LongSupplier;

public class ValidatorForCanaryPBM implements
        ValidatorListener {
    private final ValidatorForCircuitBreakers vcbs;
    private final PublishWorkflowStager publishStager;
    private final DefaultHollowPublishJobCreator jobCreator;
    private final String vip;
    private final LongSupplier previousVersion;

    public ValidatorForCanaryPBM(
            ValidatorForCircuitBreakers vcbs,
            PublishWorkflowStager publishStager,
            DefaultHollowPublishJobCreator jobCreator,
            String vip,
            LongSupplier previousVersion) {
        this.vcbs = vcbs;
        this.publishStager = publishStager;
        this.jobCreator = jobCreator;
        this.vip = vip;
        this.previousVersion = previousVersion;
    }


    // ValidatorListener

    @Override
    public String getName() {
        return "Canary Playback Monkey";
    }

    @Override
    public ValidationResult onValidate(
            HollowProducer.ReadState readState) {
        if (vcbs.failed) {
            return ValidationResult.from(this).passed("SKIPPED: circuit breaker validators failed");
        }

        long version = readState.getVersion();

        ValidationResult r = null;
        try {
            r = validate(readState, vip, version);
            return r;
        } catch (Exception e) {
            jobCreator.getContext().getLogger().error(PlaybackMonkey, "Validation error", e);
            throw e;
        } finally {
            if (r == null || !r.isPassed()) {
                CanaryRollbackJob canaryRollbackJob = jobCreator.createCanaryRollbackJob(
                        vip, version, previousVersion.getAsLong(),
                        null);
                canaryRollbackJob.executeJob();
            }
        }
    }

    private ValidationResult validate(HollowProducer.ReadState readState, String vip, long version) {
        HollowReadStateEngine readStateEngine = readState.getStateEngine();

        HollowBlobBeforeCanaryAnnounceJob beforeCanaryAnnounceJob =
                (HollowBlobBeforeCanaryAnnounceJob) jobCreator.createBeforeCanaryAnnounceJob(
                        vip, version,
                        null, Collections.emptyList());
        if (!beforeCanaryAnnounceJob.executeJob(readStateEngine)) {
            return ValidationResult.from(this).failed(beforeCanaryAnnounceJob.toString());
        }

        CanaryAnnounceJob canaryAnnounceJob = jobCreator.createCanaryAnnounceJob(
                vip, version,
                beforeCanaryAnnounceJob);
        if (!canaryAnnounceJob.executeJob()) {
            return ValidationResult.from(this).failed(canaryAnnounceJob.toString());
        }

        HollowBlobAfterCanaryAnnounceJob afterCanaryAnnounceJob =
                (HollowBlobAfterCanaryAnnounceJob) jobCreator.createAfterCanaryAnnounceJob(
                        vip, version,
                        canaryAnnounceJob);
        if (!afterCanaryAnnounceJob.executeJob(readStateEngine)) {
            return ValidationResult.from(this).failed(afterCanaryAnnounceJob.toString());
        }

        CassandraCanaryValidationJob validationJob =
                (CassandraCanaryValidationJob) jobCreator.createCanaryValidationJob(
                        vip, version,
                        beforeCanaryAnnounceJob, afterCanaryAnnounceJob);
        if (!validationJob.executeJob(readStateEngine)) {
            return ValidationResult.from(this).failed(validationJob.toString());
        }

        return ValidationResult.from(this).passed();
    }
}
