package com.bytabit.mobile.trade.manager;

import com.bytabit.mobile.common.RetryWithDelay;
import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.trade.model.Trade;
import com.bytabit.mobile.trade.model.TradeStorageResource;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TradeStorage {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static final String TRADES_PATH = AppConfig.getPrivateStorage().getPath() + File.separator +
            "trades" + File.separator;

    private static final String CURRENT_TRADE_JSON = "currentTrade.json";

    private final InputStreamInputConverter<TradeStorageResource> tradeInputConverter;

    private final OutputStreamOutputConverter<TradeStorageResource> tradeOutputConverter;

    TradeStorage() {

        // create a JSON converters that convert a JSON object to/from a trade object
        tradeInputConverter = new JsonInputConverter<>(TradeStorageResource.class);
        tradeOutputConverter = new JsonOutputConverter<>(TradeStorageResource.class);
    }

    void createTradesDir() {

        File tradesDir = new File(TRADES_PATH);
        if (!tradesDir.exists()) {
            tradesDir.mkdirs();
        }
    }

    Observable<List<Trade>> getAll() {

        File tradesDir = new File(TRADES_PATH);

        if (!tradesDir.exists()) {
            tradesDir.mkdirs();
        }

        return Observable.fromArray(tradesDir.list())
                .flatMapMaybe(this::read)
                .toList().toObservable();
    }

    Single<Trade> write(Trade trade) {

        return Single.<Trade>create(source -> {
            File tradeFile = new File(TRADES_PATH + trade.getEscrowAddress() + File.separator + CURRENT_TRADE_JSON);
            try {
                File dir = new File(TRADES_PATH + trade.getEscrowAddress());
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                // create a FileClient to the specified File
                FileClient fileClient = FileClient.create(tradeFile);
                ObjectDataWriter<TradeStorageResource> objectWriter = fileClient.createObjectDataWriter(tradeOutputConverter);
                // write an object with an ObjectDataWriter created from the FileClient
                objectWriter.writeObject(TradeStorageResource.fromTrade(trade)).map(TradeStorageResource::toTrade).ifPresent(source::onSuccess);
            } catch (IOException e) {
                source.onError(e);
            }
        })
                .retryWhen(new RetryWithDelay(3, 500, TimeUnit.MILLISECONDS))
                .doOnError(t -> log.error("write error: {}", t.getMessage()));
    }

    Maybe<Trade> read(String escrowAddress) {

        return Maybe.<Trade>create(source -> {
            try {
                File tradeFile = new File(TRADES_PATH + escrowAddress + File.separator + CURRENT_TRADE_JSON);
                if (tradeFile.exists()) {
                    // create a FileClient to the specified File
                    FileClient fileClient = FileClient.create(tradeFile);
                    // retrieve an object from an ObjectDataReader created from the FileClient
                    ObjectDataReader<TradeStorageResource> objectReader = fileClient.createObjectDataReader(tradeInputConverter);
                    source.onSuccess(TradeStorageResource.toTrade(objectReader.readObject()));
                } else {
                    source.onComplete();
                }
            } catch (Exception ex) {
                source.onError(ex);
            }
        })
                .retryWhen(new RetryWithDelay(3, 500, TimeUnit.MILLISECONDS))
                .doOnError(t -> log.error("read error: {}", t.getMessage()));
    }
}
