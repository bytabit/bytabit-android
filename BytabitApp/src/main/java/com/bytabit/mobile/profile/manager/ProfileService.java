package com.bytabit.mobile.profile.manager;

import com.bytabit.mobile.config.AppConfig;
import com.bytabit.mobile.profile.model.Profile;
import com.gluonhq.connect.GluonObservableObject;
import com.gluonhq.connect.provider.DataProvider;
import com.gluonhq.connect.provider.ObjectDataWriter;
import com.gluonhq.connect.provider.RestClient;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;

public class ProfileService {

    Single<Profile> putProfile(String pubkey, Profile profile) {

        return Single.create((SingleEmitter<Profile> source) -> {

            RestClient putRestClient = RestClient.create()
                    .host(AppConfig.getBaseUrl())
                    .path(String.format("/profiles/%s", pubkey))
                    .method("PUT")
                    .contentType("application/json");

            ObjectDataWriter<Profile> dataWriter = putRestClient.createObjectDataWriter(Profile.class);
            GluonObservableObject<Profile> putProfile = DataProvider.storeObject(profile, dataWriter);

            putProfile.addListener((o, ov, nv) -> source.onSuccess(nv));

            putProfile.exceptionProperty().addListener((o, ov, nv) -> source.onError(nv));
        });
    }
}
