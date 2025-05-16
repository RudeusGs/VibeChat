package com.rudeusgrey.vibechat;

import android.content.Context;
import android.media.MediaPlayer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
    private DatabaseReference groupsRef;
    private MediaPlayer mediaPlayer;
    private ImageButton currentPlayingButton;

    public MessageAdapter(List<ChatActivity.Message> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
        this.usersRef = FirebaseDatabase.getInstance().getReference("users");
        this.chatsRef = FirebaseDatabase.getInstance().getReference("chats");
        this.groupsRef = FirebaseDatabase.getInstance().getReference("groups");
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

        if (isSentByCurrentUser) {
            holder.sentMessageLayout.setVisibility(View.VISIBLE);
            holder.receivedMessageLayout.setVisibility(View.GONE);

            if (message.imageUrl != null) {
                holder.sentImageView.setVisibility(View.VISIBLE);
                holder.sentMessageTextView.setVisibility(View.GONE);
                holder.sentAudioLayout.setVisibility(View.GONE);
                Glide.with(holder.itemView.getContext())
                        .load(message.imageUrl)
                        .into(holder.sentImageView);
            } else if (message.content != null && message.content.startsWith("Audio: ")) {
                holder.sentImageView.setVisibility(View.GONE);
                holder.sentMessageTextView.setVisibility(View.GONE);
                holder.sentAudioLayout.setVisibility(View.VISIBLE);
                holder.sentAudioLabel.setText("Voice message");
                holder.sentPlayButton.setImageResource(R.drawable.ic_play);
                holder.sentPlayButton.setOnClickListener(v -> playAudio(message.content.replace("Audio: ", ""), holder.sentPlayButton, holder.itemView.getContext()));
            } else {
                holder.sentImageView.setVisibility(View.GONE);
                holder.sentAudioLayout.setVisibility(View.GONE);
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

            holder.sentPinIcon.setVisibility(message.isPinned ? View.VISIBLE : View.GONE);
        } else {
            holder.sentMessageLayout.setVisibility(View.GONE);
            holder.receivedMessageLayout.setVisibility(View.VISIBLE);

            if (message.imageUrl != null) {
                holder.receivedImageView.setVisibility(View.VISIBLE);
                holder.receivedMessageTextView.setVisibility(View.GONE);
                holder.receivedAudioLayout.setVisibility(View.GONE);
                Glide.with(holder.itemView.getContext())
                        .load(message.imageUrl)
                        .into(holder.receivedImageView);
            } else if (message.content != null && message.content.startsWith("Audio: ")) {
                holder.receivedImageView.setVisibility(View.GONE);
                holder.receivedMessageTextView.setVisibility(View.GONE);
                holder.receivedAudioLayout.setVisibility(View.VISIBLE);
                holder.receivedAudioLabel.setText("Voice message");
                holder.receivedPlayButton.setImageResource(R.drawable.ic_play);
                holder.receivedPlayButton.setOnClickListener(v -> playAudio(message.content.replace("Audio: ", ""), holder.receivedPlayButton, holder.itemView.getContext()));
            } else {
                holder.receivedImageView.setVisibility(View.GONE);
                holder.receivedAudioLayout.setVisibility(View.GONE);
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

            holder.receivedPinIcon.setVisibility(message.isPinned ? View.VISIBLE : View.GONE);
        }

        holder.itemView.setOnLongClickListener(v -> {
            showMessageOptions(holder.itemView.getContext(), message, position, holder.itemView);
            return true;
        });
    }

    private void playAudio(String audioUrl, ImageButton playButton, Context context) {
        // If the same button is clicked while playing, stop the audio
        if (currentPlayingButton == playButton && mediaPlayer != null && mediaPlayer.isPlaying()) {
            stopAudio(playButton);
            return;
        }

        // Stop any ongoing playback
        stopAudio(currentPlayingButton);

        currentPlayingButton = playButton;
        playButton.setImageResource(R.drawable.ic_stop); // Show stop icon while playing

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(audioUrl);
            mediaPlayer.setOnPreparedListener(mp -> {
                mediaPlayer.start();
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                stopAudio(playButton);
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                stopAudio(playButton);
                Toast.makeText(context, "Không thể phát âm thanh", Toast.LENGTH_SHORT).show();
                return true;
            });
            mediaPlayer.prepareAsync(); // Use async to avoid blocking the UI thread
        } catch (Exception e) {
            e.printStackTrace();
            stopAudio(playButton);
            Toast.makeText(context, "Lỗi phát âm thanh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void stopAudio(ImageButton playButton) {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (playButton != null) {
            playButton.setImageResource(R.drawable.ic_play); // Revert to play icon
        }
        currentPlayingButton = null;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        stopAudio(currentPlayingButton);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    private void showMessageOptions(Context context, ChatActivity.Message message, int position, View anchor) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Tùy chọn tin nhắn");

        List<String> options = new ArrayList<>();
        options.add("Thả cảm xúc");
        options.add(message.isPinned ? "Bỏ ghim" : "Ghim");

        builder.setItems(options.toArray(new String[0]), (dialog, which) -> {
            if (which == 0) {
                showReactionMenu(context, message, position, anchor);
            } else if (which == 1) {
                if (message.isPinned) {
                    unpinMessage(message, position);
                } else {
                    pinMessage(message, position);
                }
            }
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void pinMessage(ChatActivity.Message message, int position) {
        String chatId = getChatId(currentUserId, message.senderId);
        String messageId = message.messageId;
        if (messageId != null) {
            message.isPinned = true;
            chatsRef.child(chatId).child("messages").child(messageId).child("isPinned").setValue(true);
            notifyDataSetChanged();
        }
    }

    private void unpinMessage(ChatActivity.Message message, int position) {
        String chatId = getChatId(currentUserId, message.senderId);
        String messageId = message.messageId;
        if (messageId != null) {
            message.isPinned = false;
            chatsRef.child(chatId).child("messages").child(messageId).child("isPinned").setValue(false);
            notifyDataSetChanged();
        }
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

    public void createGroupChat(String groupName, List<String> memberIds, OnGroupCreatedListener listener) {
        String groupId = groupsRef.push().getKey();
        if (groupId == null) {
            listener.onGroupCreationFailed("Failed to generate group ID");
            return;
        }

        if (!memberIds.contains(currentUserId)) {
            memberIds.add(currentUserId);
        }

        Map<String, Object> groupData = new HashMap<>();
        groupData.put("groupName", groupName);
        groupData.put("createdAt", System.currentTimeMillis());
        groupData.put("members", memberIds);

        groupsRef.child(groupId).setValue(groupData, (error, ref) -> {
            if (error != null) {
                listener.onGroupCreationFailed(error.getMessage());
            } else {
                listener.onGroupCreated(groupId);
            }
        });

        for (String memberId : memberIds) {
            usersRef.child(memberId).child("groups").child(groupId).setValue(true);
        }
    }

    public interface OnGroupCreatedListener {
        void onGroupCreated(String groupId);
        void onGroupCreationFailed(String errorMessage);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ConstraintLayout receivedMessageLayout, sentMessageLayout;
        ConstraintLayout receivedAudioLayout, sentAudioLayout;
        TextView receivedSenderTextView, receivedMessageTextView, receivedTimeTextView;
        TextView sentMessageTextView, sentTimeTextView;
        TextView receivedAudioLabel, sentAudioLabel;
        ImageView receivedReactionImageView, sentReactionImageView;
        ImageView receivedImageView, sentImageView;
        ImageView receivedPinIcon, sentPinIcon;
        ImageButton receivedPlayButton, sentPlayButton;

        public ViewHolder(View itemView) {
            super(itemView);
            receivedMessageLayout = itemView.findViewById(R.id.receivedMessageLayout);
            sentMessageLayout = itemView.findViewById(R.id.sentMessageLayout);
            receivedAudioLayout = itemView.findViewById(R.id.receivedAudioLayout);
            sentAudioLayout = itemView.findViewById(R.id.sentAudioLayout);
            receivedSenderTextView = itemView.findViewById(R.id.receivedSenderTextView);
            receivedMessageTextView = itemView.findViewById(R.id.receivedMessageTextView);
            receivedTimeTextView = itemView.findViewById(R.id.receivedTimeTextView);
            sentMessageTextView = itemView.findViewById(R.id.sentMessageTextView);
            sentTimeTextView = itemView.findViewById(R.id.sentTimeTextView);
            receivedAudioLabel = itemView.findViewById(R.id.receivedAudioLabel);
            sentAudioLabel = itemView.findViewById(R.id.sentAudioLabel);
            receivedReactionImageView = itemView.findViewById(R.id.receivedReactionImageView);
            sentReactionImageView = itemView.findViewById(R.id.sentReactionImageView);
            receivedImageView = itemView.findViewById(R.id.receivedImageView);
            sentImageView = itemView.findViewById(R.id.sentImageView);
            receivedPinIcon = itemView.findViewById(R.id.receivedPinIcon);
            sentPinIcon = itemView.findViewById(R.id.sentPinIcon);
            receivedPlayButton = itemView.findViewById(R.id.receivedPlayButton);
            sentPlayButton = itemView.findViewById(R.id.sentPlayButton);
        }
    }
}