package com.bytabit.mobile.wallet.evt;

import org.bitcoinj.core.Block;
import org.bitcoinj.core.FilteredBlock;
import org.bitcoinj.core.Peer;

public class BlockDownloaded implements BlockDownloadEvent {

    final private Peer peer;
    final private Block block;
    final private FilteredBlock filteredBlock;
    final private Integer blocksLeft;

    public BlockDownloaded(Peer peer, Block block, FilteredBlock filteredBlock, Integer blocksLeft) {
        this.peer = peer;
        this.block = block;
        this.filteredBlock = filteredBlock;
        this.blocksLeft = blocksLeft;
    }

    public Peer getPeer() {
        return peer;
    }

    public Block getBlock() {
        return block;
    }

    public FilteredBlock getFilteredBlock() {
        return filteredBlock;
    }

    public Integer getBlocksLeft() {
        return blocksLeft;
    }
}
