# VibeChat — Real-time Chat App (Android + Firebase)

[![Android](https://img.shields.io/badge/Android-27%2B-brightgreen)](https://developer.android.com/)
[![Language](https://img.shields.io/badge/Language-Java-blue)](https://www.java.com/)
[![Firebase](https://img.shields.io/badge/Backend-Firebase-orange)](https://firebase.google.com/)
[![UI](https://img.shields.io/badge/UI-Material%20Design-8A2BE2)](https://m3.material.io/)

**VibeChat** là ứng dụng nhắn tin thời gian thực trên Android, tập trung vào trải nghiệm chat cá nhân & nhóm: nhắn tin realtime, gửi ảnh, ghi âm (prototype), reaction, ghim tin nhắn, và thông báo đẩy (local notification) khi có tin mới.

---

## ✨ Highlights (What recruiters care about)

- **Realtime messaging** với Firebase Realtime Database (listener cập nhật UI tức thì)
- **Authentication** với Firebase Auth (Register/Login)
- **Social features**: kết bạn, lời mời kết bạn, danh sách bạn bè, tạo nhóm chat
- **Rich chat**: gửi ảnh, voice message (prototype), reaction, pinned messages
- **Notifications**: tạo notification channel + hiển thị thông báo khi có tin nhắn mới
- **Clean Android fundamentals**: RecyclerView adapters, permissions, Activity navigation, Material UI

---

## 🚀 Features

### 1) Account & Profile
- Đăng ký / đăng nhập bằng Email & Password (Firebase Auth)
- Màn hình thông tin cá nhân, chỉnh sửa thông tin cơ bản (name, DOB, gender)

### 2) Friends & Requests
- Tìm kiếm người dùng theo username
- Gửi / hủy lời mời kết bạn
- Accept / reject lời mời kết bạn
- Danh sách bạn bè + trạng thái hiển thị theo dữ liệu realtime

### 3) 1–1 Chat
- Nhắn tin realtime
- Gửi ảnh (upload qua Imgur API, sau đó lưu URL vào DB)
- Ghi âm & gửi voice message (**prototype**)
- Thả reaction (like/love/haha/wow/sad/angry)
- Ghim / bỏ ghim tin nhắn + khu vực pinned messages
- Tự động cuộn xuống tin mới

### 4) Group Chat
- Tạo nhóm chat từ danh sách bạn bè
- Nhắn tin trong nhóm, xem last message + timestamp

### 5) Notifications
- Tạo notification channel
- Hiển thị thông báo tin nhắn mới khi không ở màn hình chat hiện tại
- Tránh thông báo trùng bằng cơ chế đánh dấu messageId đã xử lý

---

## 🧰 Tech Stack

**Android**
- Java 11
- Gradle Kotlin DSL (`build.gradle.kts`)
- Material Components, RecyclerView

**Backend**
- Firebase Authentication
- Firebase Realtime Database
- (Dependency có sẵn) Firebase Storage *(roadmap / có thể mở rộng)*

**Networking & Media**
- OkHttp (gọi Imgur API)
- Glide (load ảnh)
- MediaRecorder + MediaPlayer (voice message prototype)

---

## 🏗️ Architecture (High level)

- **Activities**: `LoginActivity`, `RegisterActivity`, `MainActivity`, `ChatActivity`, `GroupChatActivity`, `FriendsListActivity`, `InfoActivity`
- **Adapters**: `MessageAdapter`, `FriendAdapter`, `GroupAdapter`, `FriendRequestAdapter`, `FriendSelectAdapter`, `SearchResultAdapter`
- **Firebase nodes** (conceptual):
  - `users/`
  - `friends/{uid}/`
  - `friend_requests/{uid}/`
  - `chats/{chatId}/messages/`
  - `groups/{groupId}/messages/`

> Note: `chatId` được tạo theo rule sắp xếp uid để đảm bảo 2 user luôn map về cùng 1 phòng chat.

---

## 📦 Getting Started

### Requirements
- Android Studio (khuyến nghị bản mới)
- JDK 11
- Android SDK (minSdk 27+)

### 1) Clone project
```bash
git clone https://github.com/<your-username>/vibechat.git
cd vibechat
