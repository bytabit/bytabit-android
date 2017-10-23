package com.bytabit.mobile.wallet.evt;

import org.joda.time.LocalDateTime;

public class BlockDownloadProgressBuilder {
    private Double pct;
    private Integer blocksSoFar;
    private LocalDateTime date;

    public BlockDownloadProgressBuilder pct(Double pct) {
        this.pct = pct;
        return this;
    }

    public BlockDownloadProgressBuilder blocksSoFar(Integer blocksSoFar) {
        this.blocksSoFar = blocksSoFar;
        return this;
    }

    public BlockDownloadProgressBuilder date(LocalDateTime date) {
        this.date = date;
        return this;
    }

    public BlockDownloadProgress build() {
        return new BlockDownloadProgress(pct, blocksSoFar, date);
    }
}