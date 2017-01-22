package com.bytabit.ft;

import com.bytabit.ft.nav.evt.NavEvent;
import com.bytabit.ft.wallet.evt.WalletEvent;
import rx.javafx.sources.CompositeObservable;

public class EventObservables {

    private final static CompositeObservable<NavEvent> navEvents = new CompositeObservable<>();

    private final static CompositeObservable<WalletEvent> walletEvents = new CompositeObservable<>();

    private EventObservables() {
    }

    public static CompositeObservable<NavEvent> getNavEvents() {
        return navEvents;
    }

    public static CompositeObservable<WalletEvent> getWalletEvents() {
        return walletEvents;
    }
}
