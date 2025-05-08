package com.rudeusgrey.vibechat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {
    private List<ChatActivity.Message> messages;
    private String currentUserId;
    private DatabaseReference usersRef;
    private Map<String, String> userNameCache = new HashMap<>();

    public MessageAdapter(List<ChatActivity.Message> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
        this.usersRef = FirebaseDatabase.getInstance().getReference("users");
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ChatActivity.Message message = messages.get(position);
        boolean isSentByCurrentUser = message.senderId.equals(currentUserId);

        // Hiển thị layout tương ứng: gửi hoặc nhận
        if (isSentByCurrentUser) {
            holder.sentMessageLayout.setVisibility(View.VISIBLE);
            holder.receivedMessageLayout.setVisibility(View.GONE);

            // Hiển thị nội dung tin nhắn gửi
            holder.sentMessageTextView.setText(message.content);

            // Hiển thị thời gian gửi
            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(message.timestamp));
            holder.sentTimeTextView.setText(time);
        } else {
            holder.sentMessageLayout.setVisibility(View.GONE);
            holder.receivedMessageLayout.setVisibility(View.VISIBLE);

            // Hiển thị nội dung tin nhắn nhận
            holder.receivedMessageTextView.setText(message.content);

            // Hiển thị thời gian nhận
            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(message.timestamp));
            holder.receivedTimeTextView.setText(time);

            // Lấy tên người gửi từ cache hoặc Firebase
            if (userNameCache.containsKey(message.senderId)) {
                holder.receivedSenderTextView.setText(userNameCache.get(message.senderId));
            } else {
                usersRef.child(message.senderId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        RegisterActivity.User user = snapshot.getValue(RegisterActivity.User.class);
                        if (user != null) {
                            userNameCache.put(message.senderId, user.username);
                            holder.receivedSenderTextView.setText(user.username);
                        } else {
                            holder.receivedSenderTextView.setText("Unknown");
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        holder.receivedSenderTextView.setText("Unknown");
                    }
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ConstraintLayout receivedMessageLayout, sentMessageLayout;
        TextView receivedSenderTextView, receivedMessageTextView, receivedTimeTextView;
        TextView sentMessageTextView, sentTimeTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            receivedMessageLayout = itemView.findViewById(R.id.receivedMessageLayout);
            sentMessageLayout = itemView.findViewById(R.id.sentMessageLayout);
            receivedSenderTextView = itemView.findViewById(R.id.receivedSenderTextView);
            receivedMessageTextView = itemView.findViewById(R.id.receivedMessageTextView);
            receivedTimeTextView = itemView.findViewById(R.id.receivedTimeTextView);
            sentMessageTextView = itemView.findViewById(R.id.sentMessageTextView);
            sentTimeTextView = itemView.findViewById(R.id.sentTimeTextView);
        }
    }
}