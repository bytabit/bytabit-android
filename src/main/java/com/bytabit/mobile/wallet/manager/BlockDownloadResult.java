package com.bytabit.mobile.wallet.manager;

import com.bytabit.mobile.common.AbstractResult;

public class BlockDownloadResult extends AbstractResult<BlockDownloadResult.Type> {

    public enum Type {
        UPDATE, DONE, ERROR
    }

    private Double percent;

    static BlockDownloadResult progress(Double percent) {
        return new BlockDownloadResult(Type.UPDATE, percent, null);
    }

    static BlockDownloadResult done() {
        return new BlockDownloadResult(Type.DONE, 1.0, null);
    }

    static BlockDownloadResult error(Throwable throwable) {
        return new BlockDownloadResult(Type.ERROR, null, throwable);
    }

    private BlockDownloadResult(Type type, Double percent, Throwable error) {
        super(type, error);
        this.percent = percent;
    }

    public Double getPercent() {
        return percent;
    }
}
