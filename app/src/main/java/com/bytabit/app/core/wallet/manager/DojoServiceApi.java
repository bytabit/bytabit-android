/*
 * Copyright 2019 Bytabit AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytabit.app.core.wallet.manager;

import com.bytabit.app.core.wallet.model.DojoAuthResponse;
import com.bytabit.app.core.wallet.model.DojoHdAccountResponse;
import com.bytabit.app.core.wallet.model.DojoResponse;

import io.reactivex.Single;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface DojoServiceApi {

    /**
     * Authentication
     * <p>
     * Authenticate to the backend by providing the API key expected by the server. If
     * authentication succeeds, the endpoint returns a json embedding an access token and a refresh
     * token (JSON Web Tokens). The access token must be passed as an argument or in the
     * Authorization HTTP header for all later calls to the backend (account & pushtx REST API +
     * websockets). The refresh token must be passed as an argument or in the Authorization HTTP
     * header for later calls to /auth/refresh allowing to generate a new access token.
     *
     * @param apikey the API key securing access to the backend.
     * @return access and refresh tokens
     */
    @FormUrlEncoded
    @POST("auth/login")
    Single<DojoAuthResponse> login(@Field("apikey") String apikey);

    // GET /multiaddr?active=...[&new=...][&bip49=...][&bip84=...][&pubkey=...]

    // GET /unspent?active=...&new=...&bip49=...&bip84=...&pubkey=...

    // GET /xpub/:xpub

    /**
     * Get HD Account
     * <p>
     * Request details about an HD account. If account does not exist, it must be created with POST
     * /xpub, and this call will return an error.
     * <p>
     * Data returned includes the unspent balance, the next unused address indices for external and
     * internal chains, the derivation path of addresses, and the created timestamp when the server
     * first saw this HD account.
     *
     * @param xpub the extended public key for the HD account
     * @return dojo response with hd account data or error
     */
    @GET("xpub/{xpub}")
    Single<DojoHdAccountResponse> getHdAccount(@Path("xpub") String xpub);

    /**
     * Add HD Account
     * <p>
     * Notify the server of the new HD account for tracking. When new accounts are sent, there is no
     * need to rescan the addresses for existing transaction activity. SegWit support is provided
     * via BIP49 or BIP84.
     * <p>
     * Response time for restored accounts might be long if there is much previous activity.
     *
     * @param xpub   extended public key for the HD account.
     * @param type   whether this is a newly-created account or one being restored. Valid values: 'new' and 'restore'.
     * @param segwit (optional) what type of SegWit support for this xpub, if any. Valid values: 'bip49' and 'bip84'.
     * @param force  (optional) force an override of derivation scheme even if xpub is locked. Used for 'restore' operation.
     * @return status 'ok' if successful, 'error' otherwise.
     */
    @FormUrlEncoded
    @POST("xpub")
    Single<DojoResponse> addHdAccount(@Field("xpub") String xpub, @Field("type") String type,
                                      @Field("segwit") String segwit, @Field("force") Boolean force);

    // POST /xpub/:xpub/lock

    // GET /tx/:txid
    // GET /tx/:txid?fees=1

    // GET /txs?active=...

    // GET /header/:hash

    // GET /fees

    // POST /pushtx/


}
