package io.aware.coala

import io.aware.coala.Constants.BASE_URL
import io.aware.coala.model.UploadResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface RemoteApi {
    @Multipart
    @POST("upload_audio") //change the endpoint
    suspend fun uploadAudio(
        @Part audio: MultipartBody.Part
    ): Response<UploadResponse>

    companion object{
        operator fun invoke(): RemoteApi{
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(RemoteApi::class.java)
        }
    }
}