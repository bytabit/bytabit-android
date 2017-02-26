package com.bytabit.ft.wallet.evt;

import org.joda.time.LocalDateTime;

public class DownloadProgress extends WalletEvent {
    final private Double pct;
    final private Integer blocksSoFar;
    final private LocalDateTime date;

    public DownloadProgress(Double pct, Integer blocksSoFar, LocalDateTime date) {
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

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("DownloadProgress{");
        sb.append("pct=").append(pct);
        sb.append(", blocksSoFar=").append(blocksSoFar);
        sb.append(", date=").append(date);
        sb.append('}');
        return sb.toString();
    }
}
