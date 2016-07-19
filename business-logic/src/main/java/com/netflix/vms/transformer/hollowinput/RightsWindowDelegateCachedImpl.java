package com.netflix.vms.transformer.hollowinput;

import com.netflix.hollow.objects.delegate.HollowObjectAbstractDelegate;
import com.netflix.hollow.read.dataaccess.HollowObjectTypeDataAccess;
import com.netflix.hollow.HollowObjectSchema;
import com.netflix.hollow.read.customapi.HollowTypeAPI;
import com.netflix.hollow.objects.delegate.HollowCachedDelegate;

@SuppressWarnings("all")
public class RightsWindowDelegateCachedImpl extends HollowObjectAbstractDelegate implements HollowCachedDelegate, RightsWindowDelegate {

    private final Long startDate;
    private final Long endDate;
    private final int contractIdsOrdinal;
    private final int contractIdsExtOrdinal;
   private RightsWindowTypeAPI typeAPI;

    public RightsWindowDelegateCachedImpl(RightsWindowTypeAPI typeAPI, int ordinal) {
        this.startDate = typeAPI.getStartDateBoxed(ordinal);
        this.endDate = typeAPI.getEndDateBoxed(ordinal);
        this.contractIdsOrdinal = typeAPI.getContractIdsOrdinal(ordinal);
        this.contractIdsExtOrdinal = typeAPI.getContractIdsExtOrdinal(ordinal);
        this.typeAPI = typeAPI;
    }

    public long getStartDate(int ordinal) {
        return startDate.longValue();
    }

    public Long getStartDateBoxed(int ordinal) {
        return startDate;
    }

    public long getEndDate(int ordinal) {
        return endDate.longValue();
    }

    public Long getEndDateBoxed(int ordinal) {
        return endDate;
    }

    public int getContractIdsOrdinal(int ordinal) {
        return contractIdsOrdinal;
    }

    public int getContractIdsExtOrdinal(int ordinal) {
        return contractIdsExtOrdinal;
    }

    @Override
    public HollowObjectSchema getSchema() {
        return typeAPI.getTypeDataAccess().getSchema();
    }

    @Override
    public HollowObjectTypeDataAccess getTypeDataAccess() {
        return typeAPI.getTypeDataAccess();
    }

    public RightsWindowTypeAPI getTypeAPI() {
        return typeAPI;
    }

    public void updateTypeAPI(HollowTypeAPI typeAPI) {
        this.typeAPI = (RightsWindowTypeAPI) typeAPI;
    }

}