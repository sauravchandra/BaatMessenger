package saurav.chandra.baatmessenger;

class ChatL {

    String user_uid;
    String user_phonenumber;
    String user_name;
    String user_last_message;
    String user_last_message_sender;
    String user_last_message_state;
    String user_last_message_time;
    String user_chat_with_fcm_id;
    String user_unread_msg;

    ChatL(String user_uid, String user_phonenumber, String user_name, String user_last_message, String user_last_message_sender, String user_last_message_state, String user_last_message_time, String user_chat_with_fcm_id, String user_unread_msg) {
        this.user_uid = user_uid;
        this.user_phonenumber = user_phonenumber;
        this.user_name = user_name;
        this.user_last_message = user_last_message;
        this.user_last_message_sender = user_last_message_sender;
        this.user_last_message_state = user_last_message_state;
        this.user_last_message_time = user_last_message_time;
        this.user_chat_with_fcm_id = user_chat_with_fcm_id;
        this.user_unread_msg = user_unread_msg;
    }
}