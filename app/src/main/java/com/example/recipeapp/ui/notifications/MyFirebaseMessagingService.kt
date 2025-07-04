//package com.example.recipeapp.ui.notifications
//
//import android.annotation.SuppressLint
//import android.app.NotificationChannel
//import android.app.NotificationManager
//import android.app.PendingIntent
//import android.app.PendingIntent.FLAG_ONE_SHOT
//import android.content.Context
//import android.content.Intent
//import android.os.Build
//import android.util.Log
//import android.widget.RemoteViews
//import androidx.core.app.NotificationCompat
//import com.example.recipeapp.MainActivity
//import com.example.recipeapp.R
//import com.google.firebase.messaging.FirebaseMessagingService
//import com.google.firebase.messaging.RemoteMessage
//
//const val  channelId = "notification_channel"
//const val  channelName = "com.example.recipeapp.ui.notifications"
//
//class MyFirebaseMessagingService : FirebaseMessagingService() {
//
//
//    override fun onMessageReceived(remoteMessage: RemoteMessage) {
//        // Handle notification or data payload here
//        if(remoteMessage.getNotification()!= null){
//            generateNotification(remoteMessage.notification!!.title!!,remoteMessage.notification!!.body!!)
//        }
//
//
//        Log.d("FCM", "Message received: ${remoteMessage.data}")
//    }
//
//    override fun onNewToken(token: String) {
//        super.onNewToken(token)
//        Log.d("FCM", "New token: $token")
//        // Save this token to your Firebase DB if needed
//    }
//
//    @SuppressLint("RemoteViewLayout")
//    fun getRemoteView(title: String, message : String): RemoteViews{
//        val remoteView = RemoteViews("com.example.recipeapp.ui.notifications", R.layout.custom_notification)
//  remoteView.setTextViewText(R.id.notif_title, title)
//  remoteView.setTextViewText(R.id.notif_body, title)
//  return remoteView
//    }
//
//    fun generateNotification(title: String, message: String){
//        val intent = Intent(this, MainActivity:: class.java)
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
//
//        val pendingIntent = PendingIntent.getActivity(this, 0,intent,
//            FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
//        val builder : NotificationCompat.Builder = NotificationCompat.Builder(applicationContext, channelId)
//            .setSmallIcon(R.drawable.ic_user)
//            .setAutoCancel(true)
//            .setVibrate(longArrayOf(1000,1000,1000,1000))
//            .setOnlyAlertOnce(true)
//            .setContentIntent(pendingIntent)
//        builder = builder.setContent(getRemoteView(title,message))
//        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.0){
//            val notificationChannel = NotificationChannel(channelId, channelName,NotificationManager.IMPORTANCE_HIGH)
//         notificationManager.createNotificationChannel(notificationChannel)
//
//            notificationManager.notify(0,builder.build() )
//    }
//
//
//
//}
