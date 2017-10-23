package com.bytabit.mobile.wallet.evt;

import org.bitcoinj.core.Block;
import org.bitcoinj.core.FilteredBlock;
import org.bitcoinj.core.Peer;

public class BlockDownloadedBuilder {
    private Peer peer;
    private Block block;
    private FilteredBlock filteredBlock;
    private Integer blocksLeft;

    public BlockDownloadedBuilder peer(Peer peer) {
        this.peer = peer;
        return this;
    }

    public BlockDownloadedBuilder block(Block block) {
        this.block = block;
        return this;
    }

    public BlockDownloadedBuilder filteredBlock(FilteredBlock filteredBlock) {
        this.filteredBlock = filteredBlock;
        return this;
    }

    public BlockDownloadedBuilder blocksLeft(Integer blocksLeft) {
        this.blocksLeft = blocksLeft;
        return this;
    }

    public BlockDownloaded build() {
        return new BlockDownloaded(peer, block, filteredBlock, blocksLeft);
    }
}