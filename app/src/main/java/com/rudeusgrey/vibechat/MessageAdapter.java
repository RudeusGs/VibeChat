package com.rudeusgrey.vibechat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
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
    private DatabaseReference chatsRef;

    public MessageAdapter(List<ChatActivity.Message> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
        this.usersRef = FirebaseDatabase.getInstance().getReference("users");
        this.chatsRef = FirebaseDatabase.getInstance().getReference("chats");
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatActivity.Message message = messages.get(position);
        boolean isSentByCurrentUser = message.senderId.equals(currentUserId);

        // Hiển thị layout tương ứng: gửi hoặc nhận
        if (isSentByCurrentUser) {
            holder.sentMessageLayout.setVisibility(View.VISIBLE);
            holder.receivedMessageLayout.setVisibility(View.GONE);

            if (message.imageUrl != null) {
                holder.sentImageView.setVisibility(View.VISIBLE);
                holder.sentMessageTextView.setVisibility(View.GONE);
                Glide.with(holder.itemView.getContext())
                        .load(message.imageUrl)
                        .into(holder.sentImageView);
            } else {
                holder.sentImageView.setVisibility(View.GONE);
                holder.sentMessageTextView.setVisibility(View.VISIBLE);
                holder.sentMessageTextView.setText(message.content);
            }

            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(message.timestamp));
            holder.sentTimeTextView.setText(time);

            String userReaction = message.reactions.get(currentUserId);
            if (userReaction != null) {
                holder.sentReactionImageView.setVisibility(View.VISIBLE);
                setReactionIcon(holder.sentReactionImageView, userReaction);
            } else {
                holder.sentReactionImageView.setVisibility(View.GONE);
            }
        } else {
            holder.sentMessageLayout.setVisibility(View.GONE);
            holder.receivedMessageLayout.setVisibility(View.VISIBLE);

            if (message.imageUrl != null) {
                holder.receivedImageView.setVisibility(View.VISIBLE);
                holder.receivedMessageTextView.setVisibility(View.GONE);
                Glide.with(holder.itemView.getContext())
                        .load(message.imageUrl)
                        .into(holder.receivedImageView);
            } else {
                holder.receivedImageView.setVisibility(View.GONE);
                holder.receivedMessageTextView.setVisibility(View.VISIBLE);
                holder.receivedMessageTextView.setText(message.content);
            }

            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(message.timestamp));
            holder.receivedTimeTextView.setText(time);

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

            String userReaction = message.reactions.get(currentUserId);
            if (userReaction != null) {
                holder.receivedReactionImageView.setVisibility(View.VISIBLE);
                setReactionIcon(holder.receivedReactionImageView, userReaction);
            } else {
                holder.receivedReactionImageView.setVisibility(View.GONE);
            }
        }

        holder.itemView.setOnLongClickListener(v -> {
            showReactionMenu(v.getContext(), message, holder.getAdapterPosition(), holder.itemView);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    private void showReactionMenu(Context context, ChatActivity.Message message, int position, View anchor) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View reactionMenuView = inflater.inflate(R.layout.reaction_menu, null);

        PopupWindow reactionPopup = new PopupWindow(
                reactionMenuView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        reactionPopup.setElevation(8);
        reactionPopup.setBackgroundDrawable(null);
        reactionPopup.setOutsideTouchable(true);

        reactionMenuView.findViewById(R.id.reaction_like).setOnClickListener(v -> {
            updateReaction(message, position, "like");
            reactionPopup.dismiss();
        });

        reactionMenuView.findViewById(R.id.reaction_love).setOnClickListener(v -> {
            updateReaction(message, position, "love");
            reactionPopup.dismiss();
        });

        reactionMenuView.findViewById(R.id.reaction_haha).setOnClickListener(v -> {
            updateReaction(message, position, "haha");
            reactionPopup.dismiss();
        });

        reactionMenuView.findViewById(R.id.reaction_wow).setOnClickListener(v -> {
            updateReaction(message, position, "wow");
            reactionPopup.dismiss();
        });

        reactionMenuView.findViewById(R.id.reaction_sad).setOnClickListener(v -> {
            updateReaction(message, position, "sad");
            reactionPopup.dismiss();
        });

        reactionMenuView.findViewById(R.id.reaction_angry).setOnClickListener(v -> {
            updateReaction(message, position, "angry");
            reactionPopup.dismiss();
        });

        int[] location = new int[2];
        anchor.getLocationOnScreen(location);
        reactionPopup.showAsDropDown(anchor, 0, -anchor.getHeight() - 60);
    }

    private void updateReaction(ChatActivity.Message message, int position, String reaction) {
        String chatId = getChatId(currentUserId, message.senderId);
        String messageId = message.messageId;
        if (messageId != null) {
            if (message.reactions.containsKey(currentUserId)) {
                message.reactions.remove(currentUserId);
            }
            message.reactions.put(currentUserId, reaction);
            chatsRef.child(chatId).child("messages").child(messageId).child("reactions").setValue(message.reactions);
            notifyItemChanged(position);
        }
    }

    private void setReactionIcon(ImageView imageView, String reaction) {
        switch (reaction.toLowerCase()) {
            case "like":
                imageView.setImageResource(R.drawable.ic_like);
                break;
            case "love":
                imageView.setImageResource(R.drawable.ic_love);
                break;
            case "haha":
                imageView.setImageResource(R.drawable.ic_haha);
                break;
            case "wow":
                imageView.setImageResource(R.drawable.ic_wow);
                break;
            case "sad":
                imageView.setImageResource(R.drawable.ic_sad);
                break;
            case "angry":
                imageView.setImageResource(R.drawable.ic_angry);
                break;
        }
    }

    private String getChatId(String uid1, String uid2) {
        return uid1.compareTo(uid2) < 0 ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ConstraintLayout receivedMessageLayout, sentMessageLayout;
        TextView receivedSenderTextView, receivedMessageTextView, receivedTimeTextView;
        TextView sentMessageTextView, sentTimeTextView;
        ImageView receivedReactionImageView, sentReactionImageView;
        ImageView receivedImageView, sentImageView;

        public ViewHolder(View itemView) {
            super(itemView);
            receivedMessageLayout = itemView.findViewById(R.id.receivedMessageLayout);
            sentMessageLayout = itemView.findViewById(R.id.sentMessageLayout);
            receivedSenderTextView = itemView.findViewById(R.id.receivedSenderTextView);
            receivedMessageTextView = itemView.findViewById(R.id.receivedMessageTextView);
            receivedTimeTextView = itemView.findViewById(R.id.receivedTimeTextView);
            sentMessageTextView = itemView.findViewById(R.id.sentMessageTextView);
            sentTimeTextView = itemView.findViewById(R.id.sentTimeTextView);
            receivedReactionImageView = itemView.findViewById(R.id.receivedReactionImageView);
            sentReactionImageView = itemView.findViewById(R.id.sentReactionImageView);
            receivedImageView = itemView.findViewById(R.id.receivedImageView);
            sentImageView = itemView.findViewById(R.id.sentImageView);
        }
    }
}