package com.ciclismo.portugal.data.remote.strava

import retrofit2.Response
import retrofit2.http.*

interface StravaApiService {

    @POST("oauth/token")
    @FormUrlEncoded
    suspend fun exchangeToken(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("code") code: String,
        @Field("grant_type") grantType: String = "authorization_code"
    ): Response<StravaTokenResponse>

    @POST("oauth/token")
    @FormUrlEncoded
    suspend fun refreshToken(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("refresh_token") refreshToken: String,
        @Field("grant_type") grantType: String = "refresh_token"
    ): Response<StravaTokenResponse>

    @GET("api/v3/athlete")
    suspend fun getAthlete(
        @Header("Authorization") authorization: String
    ): Response<StravaAthlete>

    @GET("api/v3/athlete/activities")
    suspend fun getActivities(
        @Header("Authorization") authorization: String,
        @Query("after") after: Long? = null,
        @Query("before") before: Long? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 200
    ): Response<List<StravaActivity>>

    @GET("api/v3/athlete/stats/{id}")
    suspend fun getAthleteStats(
        @Header("Authorization") authorization: String,
        @Path("id") athleteId: Long
    ): Response<StravaStats>
}
