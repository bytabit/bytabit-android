package com.bytabit.mobile.profile.manager;

import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.profile.model.Profile;
import com.gluonhq.connect.provider.ListDataReader;
import com.gluonhq.connect.provider.ObjectDataWriter;
import com.gluonhq.connect.provider.RestClient;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProfileService {

    Single<Profile> put(Profile profile) {

        return Single.create((SingleEmitter<Profile> source) -> {

            RestClient putRestClient = RestClient.create()
                    .host(AppConfig.getBaseUrl())
                    .path(String.format("/profiles/%s", profile.getPubKey()))
                    .method("PUT")
                    .contentType("application/json");

            ObjectDataWriter<Profile> dataWriter = putRestClient.createObjectDataWriter(Profile.class);

            try {
                dataWriter.writeObject(profile).ifPresent(source::onSuccess);
            } catch (IOException ioe) {
                source.onError(ioe);
            }
        });
    }

    Single<List<Profile>> get() {

        return Single.create((SingleEmitter<List<Profile>> source) -> {

            RestClient getRestClient = RestClient.create()
                    .host(AppConfig.getBaseUrl())
                    .path("/profiles")
                    .method("GET")
                    .contentType("application/json");

            ListDataReader<Profile> listDataReader = getRestClient.createListDataReader(Profile.class);

            try {
                List<Profile> profiles = new ArrayList<>();
                listDataReader.iterator().forEachRemaining(profiles::add);
                source.onSuccess(profiles);
            } catch (IOException ioe) {
                source.onError(ioe);
            }
        });
    }
}
