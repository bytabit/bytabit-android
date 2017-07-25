package com.bytabit.mobile.trade;

import com.bytabit.mobile.trade.model.ArbitrateRequest;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

public interface ArbitrateRequestService {

    @Headers("Content-Type:application/json")
    @POST("/v1/arbitrateRequests/{arbitratorProfilePubKey}")
    Call<ArbitrateRequest> post(@Path("arbitratorProfilePubKey") String arbitratorProfilePubKey, @Body ArbitrateRequest arbitrateRequest);

    @Headers("Content-Type:application/json")
    @GET("/v1/arbitrateRequests/{arbitratorProfilePubKey}/{escrowAddress}")
    Call<List<ArbitrateRequest>> getAll(@Path("arbitratorProfilePubKey") String arbitratorProfilePubKey);

    @Headers("Content-Type:application/json")
    @GET("/v1/arbitrateRequests/{arbitratorProfilePubKey}/{escrowAddress}")
    Call<ArbitrateRequest> get(@Path("arbitratorProfilePubKey") String arbitratorProfilePubKey,
                               @Path("escrowAddress") String escrowAddress);
}
