package com.bytabit.mobile.profile.manager;

import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.profile.model.Profile;
import com.gluonhq.connect.provider.ObjectDataWriter;
import com.gluonhq.connect.provider.RestClient;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;

import java.io.IOException;

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
}
