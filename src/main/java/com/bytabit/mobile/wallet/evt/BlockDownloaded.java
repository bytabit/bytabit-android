package com.bytabit.mobile.wallet.evt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.FilteredBlock;
import org.bitcoinj.core.Peer;

@AllArgsConstructor
@Builder
@Getter
@ToString
public class BlockDownloaded implements BlockDownloadEvent {

    final private Peer peer;
    final private Block block;
    final private FilteredBlock filteredBlock;
    final private Integer blocksLeft;
}
