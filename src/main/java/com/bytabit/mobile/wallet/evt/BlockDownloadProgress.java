package com.bytabit.mobile.wallet.evt;

import org.joda.time.LocalDateTime;

public class BlockDownloadProgress implements BlockDownloadEvent {

    final private Double pct;
    final private Integer blocksSoFar;
    final private LocalDateTime date;

    public BlockDownloadProgress(Double pct, Integer blocksSoFar, LocalDateTime date) {
        this.pct = pct;
        this.blocksSoFar = blocksSoFar;
        this.date = date;
    }

    public Double getPct() {
        return pct;
    }

    public Integer getBlocksSoFar() {
        return blocksSoFar;
    }

    public LocalDateTime getDate() {
        return date;
    }
}
