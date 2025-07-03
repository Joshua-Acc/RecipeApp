//package com.example.recipeapp
//
//import FirebaseApi
//import retrofit2.Retrofit
//import retrofit2.converter.gson.GsonConverterFactory
//
//object RetrofitService {
//    private const val AUTH_BASE_URL = "https://identitytoolkit.googleapis.com/v1/"
//    private const val DB_BASE_URL = "https://recipeapp-bfa4b-default-rtdb.asia-southeast1.firebasedatabase.app/"
//
//    val authApi: FirebaseApi by lazy {
//        Retrofit.Builder()
//            .baseUrl(DB_BASE_URL)
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//            .create(FirebaseApi::class.java)
//    }
//
//    val dbApi: FirebaseApi by lazy {
//        Retrofit.Builder()
//            .baseUrl(DB_BASE_URL)
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//            .create(FirebaseApi::class.java)
//    }
//}