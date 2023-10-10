package com.example.node_sample

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST


/**
 * @Author: longkd
 * @Date: 2:37 PM - 10/10/2023
 */
interface ApiService {
    @POST("/api/vol_data")
    suspend fun sendData(@Body data: DataModel): Response<DataResponse>
}

data class DataModel(val key: String, val value: String)
data class DataResponse(val message: String)