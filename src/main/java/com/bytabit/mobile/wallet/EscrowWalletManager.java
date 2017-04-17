package com.bytabit.mobile.wallet;

import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.wallet.evt.WalletPurpose;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Context;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EscrowWalletManager extends WalletManager {

    private static Logger LOG = LoggerFactory.getLogger(EscrowWalletManager.class);

    public EscrowWalletManager() {
        super(AppConfig.getConfigName(), WalletPurpose.ESCROW);
    }

    public void watchTradeEscrowAddress(String tradeEscrowAddress) {
        Address address = Address.fromBase58(getNetParams(), tradeEscrowAddress);
        Context.propagate(btcContext);
        if (kit.wallet().addWatchedAddress(address, DateTime.now().getMillis() / 1000)) {
            LOG.debug("Added watch address: {}", address.toBase58());
        } else {
            LOG.error("Failed to add watch address: {}", address.toBase58());
        }
    }
}
