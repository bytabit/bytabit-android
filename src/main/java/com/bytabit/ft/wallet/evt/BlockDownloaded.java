package com.bytabit.ft.wallet.evt;

import org.bitcoinj.core.Block;
import org.bitcoinj.core.FilteredBlock;
import org.bitcoinj.core.Peer;

public class BlockDownloaded extends WalletEvent {

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

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("BlockDownloaded{");
        sb.append("peer=").append(peer);
        sb.append(", block=").append(block);
        sb.append(", filteredBlock=").append(filteredBlock);
        sb.append(", blocksLeft=").append(blocksLeft);
        sb.append('}');
        return sb.toString();
    }
}
