package com.rudeusgrey.vibechat;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class NotificationHelper {
    private static final String CHANNEL_ID = "VibeChatMessages";
    private static final String CHANNEL_NAME = "Tin nhắn VibeChat";
    private static final String CHANNEL_DESC = "Thông báo tin nhắn mới từ VibeChat";
    private static final int NOTIFICATION_ID = 1;
    private static String currentChatFriendId = null;

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESC);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    public static void setCurrentChatFriendId(String friendId) {
        currentChatFriendId = friendId;
    }

    public static void clearCurrentChatFriendId() {
        currentChatFriendId = null;
    }

    public static void showNotification(Context context, String sender, String message, String senderId) {
        // Không hiển thị thông báo nếu người dùng đang ở trong ChatActivity với sender này
        if (currentChatFriendId != null && currentChatFriendId.equals(senderId)) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        // Lấy friendId từ senderId
        FirebaseDatabase.getInstance().getReference("users").child(senderId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        String friendId = snapshot.getKey();
                        Intent intent = new Intent(context, ChatActivity.class);
                        intent.putExtra("friendId", friendId);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        PendingIntent pendingIntent = PendingIntent.getActivity(
                                context,
                                0,
                                intent,
                                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                        );

                        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                                .setSmallIcon(R.drawable.ic_notification)
                                .setContentTitle("Tin nhắn từ " + sender)
                                .setContentText(message)
                                .setPriority(NotificationCompat.PRIORITY_HIGH)
                                .setContentIntent(pendingIntent)
                                .setAutoCancel(true);

                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                        try {
                            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
                        } catch (SecurityException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }
}