package com.eyeson.sdk.di

import com.eyeson.sdk.BuildConfig
import com.eyeson.sdk.EyesonMeeting
import com.eyeson.sdk.model.meeting.base.MeetingBaseMessageDto
import com.eyeson.sdk.moshi.adapter.AsStringAdapter
import com.eyeson.sdk.moshi.adapter.DataChannelIncomingMessagesAdapter
import com.eyeson.sdk.moshi.adapter.DefaultOnDataMismatchAdapter
import com.eyeson.sdk.moshi.adapter.MeetingIncomingMessagesAdapter
import com.eyeson.sdk.moshi.adapter.SeppIncomingCommandsAdapter
import com.eyeson.sdk.network.EyesonApi
import com.eyeson.sdk.network.EyesonRestClient
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.*

internal object NetworkModule {

    private val okHttpClient: OkHttpClient by lazy { provideOkHttpClient() }
    private val retrofit: Retrofit by lazy { provideRetrofit(okHttpClient, moshi) }

    val moshi: Moshi by lazy { provideMoshi() }
    val restClient: EyesonRestClient by lazy { provideEyesonRestClient(retrofit) }


    private fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
            .add(SeppIncomingCommandsAdapter.provideSeppCommandsIncomingAdapterFactory())
            .add(DefaultOnDataMismatchAdapter.newFactory(MeetingBaseMessageDto::class.java, null))
            .add(MeetingIncomingMessagesAdapter.provideMeetingIncomingMessagesAdapterFactory())
            .add(DataChannelIncomingMessagesAdapter.provideDataChannelIncomingMessagesAdapterFactory())
            .add(AsStringAdapter.FACTORY)
            .build()
    }

    private fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor()
        logging.level =
            if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }

        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .addNetworkInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                    .addHeader("Accept", "application/json")
                val newRequest = requestBuilder.build()
                chain.proceed(newRequest)
            }
            .build()
    }

    private fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .baseUrl(EyesonMeeting.API_URL)
            .client(okHttpClient)
            .build()
    }

    private fun provideEyesonRestClient(retrofit: Retrofit): EyesonRestClient {
        val restService = retrofit.create(EyesonApi::class.java)
        return EyesonRestClient(restService)
    }

}