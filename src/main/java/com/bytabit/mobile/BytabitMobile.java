package com.bytabit.mobile;

import com.bytabit.mobile.nav.NavDrawer;
import com.bytabit.mobile.nav.evt.NavEvent;
import com.bytabit.mobile.nav.evt.QuitEvent;
import com.bytabit.mobile.offer.AddOfferView;
import com.bytabit.mobile.offer.OfferDetailsView;
import com.bytabit.mobile.offer.OffersView;
import com.bytabit.mobile.profile.PaymentView;
import com.bytabit.mobile.profile.PaymentsView;
import com.bytabit.mobile.profile.ProfileView;
import com.bytabit.mobile.trade.TradesView;
import com.bytabit.mobile.wallet.DepositView;
import com.bytabit.mobile.wallet.WalletView;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.layout.layer.SidePopupView;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.Swatch;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.javafx.sources.CompositeObservable;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class BytabitMobile extends MobileApplication {

    private static Logger LOG = LoggerFactory.getLogger(BytabitMobile.class);

    final public static String WALLET_VIEW = "Wallet";
    final public static String DEPOSIT_VIEW = "Deposit";

    final public static String OFFER_VIEW = HOME_VIEW;
    final public static String ADD_OFFER_VIEW = "AddOffer";
    final public static String OFFER_DETAILS_VIEW = "OfferDetails";

    final public static String TRADE_VIEW = "Trades";

    final public static String PROFILE_VIEW = "Profile";
    final public static String PAYMENT_VIEW = "PaymentDetails";
    final public static String ADD_PAYMENT_VIEW = "AddPaymentDetail";

    final public static String HELP_VIEW = "Contracts";

    final public static String MENU_LAYER = "SideMenu";

    final public static Executor EXECUTOR = Executors.newWorkStealingPool();

    private static CompositeObservable<NavEvent> navEventsComposite = new CompositeObservable<>();

    @Override
    public void init() {

        addViewFactory(OFFER_VIEW, () -> (View) new OffersView().getView());
        addViewFactory(ADD_OFFER_VIEW, () -> (View) new AddOfferView().getView());
        addViewFactory(OFFER_DETAILS_VIEW, () -> (View) new OfferDetailsView().getView());

        addViewFactory(TRADE_VIEW, () -> (View) new TradesView().getView());

        addViewFactory(WALLET_VIEW, () -> (View) new WalletView().getView());
        addViewFactory(DEPOSIT_VIEW, () -> (View) new DepositView().getView());

        addViewFactory(PAYMENT_VIEW, () -> (View) new PaymentsView().getView());
        addViewFactory(ADD_PAYMENT_VIEW, () -> (View) new PaymentView().getView());

        addViewFactory(PROFILE_VIEW, () -> (View) new ProfileView().getView());

//        addViewFactory(HELP_VIEW, () -> new ContractsView(HELP_VIEW));

        addLayerFactory(MENU_LAYER, () -> new SidePopupView(new NavDrawer().getDrawer()));
    }

    @Override
    public void postInit(Scene scene) {

        Swatch.ORANGE.assignTo(scene);

        scene.getStylesheets().add(BytabitMobile.class.getResource("style.css").toExternalForm());
        ((Stage) scene.getWindow()).getIcons().add(new Image(BytabitMobile.class.getResourceAsStream("/logo.png")));
    }

    @Override
    public void stop() {
        navEventsComposite.add(Observable.just(new QuitEvent()));
        LOG.debug("Stop app");
    }

    public static CompositeObservable<NavEvent> getNavEventsComposite() {
        return navEventsComposite;
    }

    public static Observable<NavEvent> getNavEvents() {
        return navEventsComposite.toObservable();
    }
}
