package com.netflix.vms.transformer.publish.workflow;

import com.netflix.hollow.api.producer.HollowProducer;
import com.netflix.hollow.api.producer.Status;
import com.netflix.hollow.api.producer.listener.PublishListener;
import com.netflix.vms.transformer.publish.workflow.job.HollowBlobPublishJob;
import com.netflix.vms.transformer.publish.workflow.job.impl.DefaultHollowPublishJobCreator;
import com.netflix.vms.transformer.publish.workflow.job.impl.FileStoreHollowBlobPublishJob;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.LongSupplier;

public class HermesPublishListener implements
        PublishListener {
    private final LongSupplier inputVersion;
    private final DefaultHollowPublishJobCreator jobCreator;
    private final String vip;
    private final LongSupplier previousVersion;

    public HermesPublishListener(
            LongSupplier inputVersion,
            DefaultHollowPublishJobCreator jobCreator,
            String vip, LongSupplier previousVersion) {
        this.inputVersion = inputVersion;
        this.jobCreator = jobCreator;
        this.vip = vip;
        this.previousVersion = previousVersion;
    }

    private long currentVersion;

    private PublishWorkflowContext ctx;


    // PublishListener

    @Override public void onNoDeltaAvailable(long version) {
    }

    @Override
    public void onPublishStart(long version) {
        // See PublishCycleListener.onCycleStart for call to jobCreator.beginStagingNewCycle()
        ctx = jobCreator.getContext();
        currentVersion = version;
    }

    @Override
    public void onBlobPublishAsync(
            CompletableFuture<HollowProducer.Blob> blob) {
        long start = System.nanoTime();

        long iv = inputVersion.getAsLong();
        long pv = previousVersion.getAsLong();
        long cv = currentVersion;
        PublishWorkflowContext context = this.ctx;
        blob.thenAccept(b -> {
            long d = System.nanoTime() - start;
            blobPublish(context, vip, b, Duration.ofNanos(d), iv, pv, cv);
        });
    }

    @Override
    public void onBlobPublish(Status status, HollowProducer.Blob blob, Duration elapsed) {
        if (status.getType() != Status.StatusType.SUCCESS) {
            return;
        }

        blobPublish(ctx, vip, blob, elapsed, inputVersion.getAsLong(), previousVersion.getAsLong(), currentVersion);
    }

    @Override public void onPublishComplete(Status status, long version, Duration elapsed) {
    }

    private static void blobPublish(
            PublishWorkflowContext ctx,
            String vip,
            HollowProducer.Blob blob, Duration elapsed,
            long inputVersion, long previousVersion, long currentVersion) {
        HollowBlobPublishJob.PublishType pt;
        switch (blob.getType()) {
            case SNAPSHOT:
                pt = HollowBlobPublishJob.PublishType.SNAPSHOT;
                break;
            case DELTA:
                pt = HollowBlobPublishJob.PublishType.DELTA;
                break;
            case REVERSE_DELTA:
            default:
                pt = HollowBlobPublishJob.PublishType.REVERSEDELTA;
                break;
        }

        FileStoreHollowBlobPublishJob publishJob = new FileStoreHollowBlobPublishJob(ctx, vip,
                inputVersion, previousVersion, currentVersion,
                pt, blob.getPath().toFile(), false);

        publishJob.executeJob(false, elapsed.toMillis());
    }
}
