package com.bytabit.mobile.wallet;

import com.gluonhq.charm.down.Platform;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.ShareService;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
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
    private WalletManager walletManager;

    public void initialize() {

        LOG.debug("initialize deposit presenter");

        depositView.showingProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                AppBar appBar = MobileApplication.getInstance().getAppBar();
                appBar.setNavIcon(MaterialDesignIcon.ARROW_BACK.button(e -> MobileApplication.getInstance().switchToPreviousView()));

                appBar.setTitleText("Deposit");
            }

            walletManager.getDepositAddress().observeOn(JavaFxScheduler.platform())
                    .subscribe(da -> {
                        bitcoinAddressLabel.setText(da.toBase58());
                        LOG.debug("deposit address: {}", da.toBase58());

                        LOG.debug("Platform is: {}", Platform.getCurrent());

                        QRCode qrCode = QRCode.from(depositAddressUri(da));

                        // TODO FT-150 Cross platform QR code generator for wallet deposits
                        if (Platform.isDesktop()) {
                            ByteArrayOutputStream outputStream = qrCode.stream();
                            Image img = new Image(new ByteArrayInputStream(outputStream.toByteArray()));
                            qrCodeImageView.setImage(img);
                        } else {
                            copyButton.setText("Share");
                        }
                        copyButton.visibleProperty().setValue(true);
                        copyButton.setOnAction((event) -> copyAddress(da));
                    });
        });
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
}
