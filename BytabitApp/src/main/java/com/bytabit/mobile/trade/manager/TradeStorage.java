package com.bytabit.mobile.trade.manager;

import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.trade.model.Trade;
import com.gluonhq.connect.converter.InputStreamInputConverter;
import com.gluonhq.connect.converter.JsonInputConverter;
import com.gluonhq.connect.converter.JsonOutputConverter;
import com.gluonhq.connect.converter.OutputStreamOutputConverter;
import com.gluonhq.connect.provider.FileClient;
import com.gluonhq.connect.provider.ObjectDataReader;
import com.gluonhq.connect.provider.ObjectDataWriter;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;

import java.io.File;
import java.io.IOException;
import java.util.List;

class TradeStorage {

    private static final String TRADES_PATH = AppConfig.getPrivateStorage().getPath() + File.separator +
            "trades" + File.separator;

    private static final String CURRENT_TRADE_JSON = "currentTrade.json";

    private final InputStreamInputConverter<Trade> tradeInputConverter;

    private final OutputStreamOutputConverter<Trade> tradeOutputConverter;

    TradeStorage() {

        // create a JSON converters that convert a JSON object to/from a trade object
        tradeInputConverter = new JsonInputConverter<>(Trade.class);
        tradeOutputConverter = new JsonOutputConverter<>(Trade.class);
    }

    void createTradesDir() {

        File tradesDir = new File(TRADES_PATH);
        if (!tradesDir.exists()) {
            tradesDir.mkdirs();
        }
    }

    Observable<List<Trade>> getStoredTrades() {

        File tradesDir = new File(TRADES_PATH);

        if (!tradesDir.exists()) {
            tradesDir.mkdirs();
        }

        return Observable.fromArray(tradesDir.list())
                .flatMapMaybe(this::readTrade)
                .toList().toObservable();
    }

    Single<Trade> writeTrade(Trade trade) {

        return Single.create(source -> {
            File tradeFile = new File(TRADES_PATH + trade.getEscrowAddress() + File.separator + CURRENT_TRADE_JSON);
            try {
                File dir = new File(TRADES_PATH + trade.getEscrowAddress());
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                // create a FileClient to the specified File
                FileClient fileClient = FileClient.create(tradeFile);
                ObjectDataWriter<Trade> objectWriter = fileClient.createObjectDataWriter(tradeOutputConverter);
                // write an object with an ObjectDataWriter created from the FileClient
                objectWriter.writeObject(trade).ifPresent(source::onSuccess);
            } catch (IOException e) {
                source.onError(e);
            }
        });
    }

    Maybe<Trade> readTrade(String escrowAddress) {

        return Maybe.create(source -> {
            try {
                File tradeFile = new File(TRADES_PATH + escrowAddress + File.separator + CURRENT_TRADE_JSON);
                if (tradeFile.exists()) {
                    // create a FileClient to the specified File
                    FileClient fileClient = FileClient.create(tradeFile);
                    // retrieve an object from an ObjectDataReader created from the FileClient
                    ObjectDataReader<Trade> objectReader = fileClient.createObjectDataReader(tradeInputConverter);
                    source.onSuccess(objectReader.readObject());
                } else {
                    source.onComplete();
                }
            } catch (Exception ex) {
                source.onError(ex);
            }
        });
    }

}
