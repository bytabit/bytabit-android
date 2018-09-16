package com.bytabit.mobile;

import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.nav.NavDrawer;
import com.bytabit.mobile.offer.ui.AddOfferView;
import com.bytabit.mobile.offer.ui.OfferDetailsView;
import com.bytabit.mobile.offer.ui.OffersView;
import com.bytabit.mobile.profile.ui.PaymentView;
import com.bytabit.mobile.profile.ui.PaymentsView;
import com.bytabit.mobile.profile.ui.ProfileView;
import com.bytabit.mobile.trade.ui.TradeDetailsView;
import com.bytabit.mobile.trade.ui.TradeDevInfoView;
import com.bytabit.mobile.trade.ui.TradesView;
import com.bytabit.mobile.wallet.ui.DepositView;
import com.bytabit.mobile.wallet.ui.WalletBackupView;
import com.bytabit.mobile.wallet.ui.WalletView;
import com.gluonhq.charm.down.Platform;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.DisplayService;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.layout.layer.SidePopupView;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.Swatch;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class BytabitMobile extends MobileApplication {

    public enum NavEvent {
        QUIT
    }

    private static final String SCALED_FONTS_CSS_FILE_PATH = AppConfig.getPrivateStorage().getPath() + File.separator +
            "scaled-fonts.css";

    public static final String WALLET_VIEW = "Wallet";
    public static final String WALLET_BACKUP_VIEW = "WalletBackup";
    public static final String DEPOSIT_VIEW = "Deposit";

    public static final String OFFERS_VIEW = "Offers";
    public static final String ADD_OFFER_VIEW = "AddOffer";
    public static final String OFFER_DETAILS_VIEW = "OfferDetails";

    public static final String TRADE_VIEW = HOME_VIEW;
    public static final String TRADE_DETAILS_VIEW = "TradeDetails";
    public static final String TRADE_DEV_INFO_VIEW = "TradeDevInfo";

    public static final String PROFILE_VIEW = "Profile";
    public static final String PAYMENT_VIEW = "PaymentDetails";
    public static final String ADD_PAYMENT_VIEW = "AddPaymentDetail";

    public static final String HELP_VIEW = "Contracts";

    public static final String MENU_LAYER = "SideMenu";

    public static final Executor EXECUTOR = Executors.newWorkStealingPool();

    private Logger log = LoggerFactory.getLogger(BytabitMobile.class);

    private static PublishSubject<NavEvent> navEventsSubject = PublishSubject.create();

    @Override
    public void init() throws Exception {
        super.init();

        addViewFactory(OFFERS_VIEW, () -> (View) new OffersView().getView());
        addViewFactory(ADD_OFFER_VIEW, () -> (View) new AddOfferView().getView());
        addViewFactory(OFFER_DETAILS_VIEW, () -> (View) new OfferDetailsView().getView());

        addViewFactory(TRADE_VIEW, () -> (View) new TradesView().getView());
        addViewFactory(TRADE_DETAILS_VIEW, () -> (View) new TradeDetailsView().getView());
        addViewFactory(TRADE_DEV_INFO_VIEW, () -> (View) new TradeDevInfoView().getView());

        addViewFactory(WALLET_VIEW, () -> (View) new WalletView().getView());
        addViewFactory(WALLET_BACKUP_VIEW, () -> (View) new WalletBackupView().getView());
        addViewFactory(DEPOSIT_VIEW, () -> (View) new DepositView().getView());

        addViewFactory(PAYMENT_VIEW, () -> (View) new PaymentsView().getView());
        addViewFactory(ADD_PAYMENT_VIEW, () -> (View) new PaymentView().getView());

        addViewFactory(PROFILE_VIEW, () -> (View) new ProfileView().getView());

        addLayerFactory(MENU_LAYER, () -> new SidePopupView(new NavDrawer().getDrawer()));
    }

    @Override
    public void postInit(Scene scene) {
        super.postInit(scene);

        Swatch.ORANGE.assignTo(scene);

        Optional<DisplayService> displayService = Services.get(DisplayService.class);

        File scaledFontsCssFile = new File(SCALED_FONTS_CSS_FILE_PATH);
        if (!scaledFontsCssFile.exists()) {

            Float screenScale = displayService.map(DisplayService::getScreenScale)
                    .map(s -> s / 2)
                    .orElse(1F);

            String scaledFontsCss = ".app-bar > .title-box > .label {\n" +
                    "    -fx-font-family: 'san-serif';\n" +
                    "    -fx-font-size: " + String.format("%.2f", screenScale * 1.5) + "em;\n" +
                    "}\n" +
                    "\n" +
                    ".dialog > .container > .label,\n" +
                    ".dialog > .container > .dialog-button-bar > .button,\n" +
                    ".title > .text, .subtitle > .text, .text-box > .label > .text,\n" +
                    ".button, .check-box, .label, .text-field,\n" +
                    ".header, .footer, .list-cell, .text-area {\n" +
                    "    -fx-font-family: 'san-serif';\n" +
                    "    -fx-font-size: " + String.format("%.2f", screenScale) + "em;\n" +
                    "}\n" +
                    "\n" +
                    ".icon-text {\n" +
                    "    -fx-font-family: 'Material Icons';\n" +
                    "    -fx-font-size: " + String.format("%.2f", screenScale * 2) + "em;\n" +
                    "}";

            // Create screen-scale.css
            try (PrintWriter printWriter = new PrintWriter(scaledFontsCssFile)) {
                printWriter.println(scaledFontsCss);
                printWriter.flush();
                log.debug("Created {} containing:\n{}", scaledFontsCssFile, scaledFontsCss);
            } catch (IOException e) {
                log.error("Could not create {}", scaledFontsCssFile, e);
            }
        }

        // Add screen-scale.css to the scene
        scene.getStylesheets().add(scaledFontsCssFile.toURI().toString());

        String formFactorSuffix = displayService
                .map(s -> s.isTablet() ? "_tablet" : "")
                .orElse("");

        log.debug("formFactorSuffix = {}", formFactorSuffix);

        String stylesheetName = String.format("bytabit_%s%s.css",
                Platform.getCurrent().name().toLowerCase(Locale.ROOT),
                formFactorSuffix);

        scene.getStylesheets().add(BytabitMobile.class.getResource(stylesheetName).toExternalForm());

        ((Stage) scene.getWindow()).getIcons().add(new Image(BytabitMobile.class.getResourceAsStream("/logo.png")));
    }

    @Override
    public void stop() {
        navEventsSubject.onNext(NavEvent.QUIT);
        log.debug("Stop app");
    }

    public static PublishSubject<NavEvent> getNavEventsSubject() {
        return navEventsSubject;
    }

    public static Observable<NavEvent> getNavEvents() {
        return navEventsSubject.share();
    }
}
