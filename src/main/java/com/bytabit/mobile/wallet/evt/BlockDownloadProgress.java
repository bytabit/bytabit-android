package com.bytabit.mobile.wallet.evt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.joda.time.LocalDateTime;

@AllArgsConstructor
@Builder
@Getter
@ToString
public class BlockDownloadProgress implements BlockDownloadEvent {
    final private Double pct;
    final private Integer blocksSoFar;
    final private LocalDateTime date;
}
