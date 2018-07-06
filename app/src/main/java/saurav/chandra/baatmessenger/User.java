package saurav.chandra.baatmessenger;

class User {

    String user_uid;
    String user_name;
    String user_phone;
    String user_chat_with_fcm_id;


    User(String user_uid, String user_name, String user_phone, String user_chat_with_fcm_id) {
        this.user_uid = user_uid;
        this.user_name = user_name;
        this.user_phone = user_phone;
        this.user_chat_with_fcm_id = user_chat_with_fcm_id;
    }

}