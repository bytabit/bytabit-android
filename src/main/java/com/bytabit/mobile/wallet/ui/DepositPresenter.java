package com.bytabit.mobile.wallet.ui;

import com.bytabit.mobile.common.Event;
import com.bytabit.mobile.common.EventLogger;
import com.bytabit.mobile.wallet.manager.WalletManager;
import com.gluonhq.charm.down.Platform;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.ShareService;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import io.reactivex.Observable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import lombok.NoArgsConstructor;
import net.glxn.qrgen.javase.QRCode;
import org.bitcoinj.core.Address;
import org.bitcoinj.uri.BitcoinURI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class DepositPresenter {

    private static Logger LOG = LoggerFactory.getLogger(DepositPresenter.class);

    @FXML
    private View depositView;

    @FXML
    private ImageView qrCodeImageView;

    @FXML
    private Label bitcoinAddressLabel;

    @FXML
    private Button copyButton;

    @Inject
    WalletManager walletManager;

    private final EventLogger eventLogger = EventLogger.of(DepositPresenter.class);

    public void initialize() {

        Observable<PresenterEvent> viewShowingEvents =
                JavaFxObservable.changesOf(depositView.showingProperty())
                        .map(showing -> {
                            if (showing.getNewVal()) {
                                return new ViewShowing();
                            } else
                                return new ViewNotShowing();
                        });

        Observable<PresenterEvent> depositEvents = viewShowingEvents
                .compose(eventLogger.logEvents()).share();

        depositEvents.ofType(ViewShowing.class).subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(e -> {
                    AppBar appBar = MobileApplication.getInstance().getAppBar();
                    appBar.setNavIcon(MaterialDesignIcon.ARROW_BACK.button(ae -> MobileApplication.getInstance().switchToPreviousView()));
                    appBar.setTitleText("Deposit");
                });

        Observable<WalletManager.GetTradeWalletDepositAddress> getTradeWalletDepositAddress = depositEvents
                .ofType(ViewShowing.class)
                .map(e -> walletManager.new GetTradeWalletDepositAddress());

        getTradeWalletDepositAddress
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .compose(eventLogger.logEvents())
                .subscribe(walletManager.getActions());

        Observable<WalletManager.WalletResult> walletResults = walletManager.getWalletResults();

        walletResults.ofType(WalletManager.TradeWalletDepositAddress.class)
                .subscribeOn(Schedulers.io())
                .observeOn(JavaFxScheduler.platform())
                .subscribe(r -> showDepositAddress(r.getAddress()));

//        depositView.showingProperty().addListener((observable, oldValue, newValue) -> {
//            if (newValue) {
//                AppBar appBar = MobileApplication.getInstance().getAppBar();
//                appBar.setNavIcon(MaterialDesignIcon.ARROW_BACK.button(e -> MobileApplication.getInstance().switchToPreviousView()));
//
//                appBar.setTitleText("Deposit");
//            }
//
//            walletManager.getDepositAddress().observeOn(JavaFxScheduler.platform())
//                    .subscribe(da -> {
//                        bitcoinAddressLabel.setText(da.toBase58());
//                        LOG.debug("deposit address: {}", da.toBase58());
//
//                        LOG.debug("Platform is: {}", Platform.getCurrent());
//
//                        QRCode qrCode = QRCode.from(depositAddressUri(da));
//
//                        // TODO FT-150 Cross platform QR code generator for wallet deposits
//                        if (Platform.isDesktop()) {
//                            ByteArrayOutputStream outputStream = qrCode.stream();
//                            Image img = new Image(new ByteArrayInputStream(outputStream.toByteArray()));
//                            qrCodeImageView.setImage(img);
//                        } else {
//                            copyButton.setText("Share");
//                        }
//                        copyButton.visibleProperty().setValue(true);
//                        copyButton.setOnAction((event) -> copyAddress(da));
//                    });
//        });
    }

    private void showDepositAddress(Address address) {
        bitcoinAddressLabel.setText(address.toBase58());
        LOG.debug("deposit address: {}", address.toBase58());

        LOG.debug("Platform is: {}", Platform.getCurrent());

        QRCode qrCode = QRCode.from(depositAddressUri(address));

        // TODO FT-150 Cross platform QR code generator for wallet deposits
        if (Platform.isDesktop()) {
            ByteArrayOutputStream outputStream = qrCode.stream();
            Image img = new Image(new ByteArrayInputStream(outputStream.toByteArray()));
            qrCodeImageView.setImage(img);
        } else {
            copyButton.setText("Share");
        }
        copyButton.visibleProperty().setValue(true);
        copyButton.setOnAction((event) -> copyAddress(address));
    }

    // TODO FT-147 make sure copy and paste works on Android and iOS
    private void copyAddress(Address address) {
        String addressStr = address.toString();
        if (Platform.isDesktop()) {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(addressStr);
            content.putHtml("<a href=" + depositAddressUri(address) + ">" + addressStr + "</a>");
            clipboard.setContent(content);
        } else {
            ShareService shareService = Services.get(ShareService.class).orElseThrow(() -> new RuntimeException("ShareService not available."));
            shareService.share(addressStr);
//            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
//            ClipData clip = ClipData.newPlainText("label", addressStr);
//            clipboard.setPrimaryClip(clip);
        }
    }

    private String depositAddressUri(Address a) {
        return BitcoinURI.convertToBitcoinURI(a, null, "Bytabit", "Bytabit deposit");
    }

//    public Image toImage(QRCode qrCode) {//BitMatrix matrix, MatrixToImageConfig config) {
//        BitMatrix matrix = qrCode.getQrWriter().encode();
//        int width = matrix.getWidth();
//        int height = matrix.getHeight();
//        PixelReader pixelReader
//        WritableImage image = new WritableImage(width, height, config.getBufferedImageColorModel());
//        int onColor = config.getPixelOnColor();
//        int offColor = config.getPixelOffColor();
//        for (int x = 0; x < width; x++) {
//            for (int y = 0; y < height; y++) {
//                image.setRGB(x, y, matrix.get(x, y) ? onColor : offColor);
//            }
//        }
//        return image;
//    }

    // Event classes

    interface PresenterEvent extends Event {
    }

    @NoArgsConstructor
    class ViewShowing implements PresenterEvent {
    }

    @NoArgsConstructor
    class ViewNotShowing implements PresenterEvent {
    }
}
