package saurav.chandra.baatmessenger;

class ChatMedia {

    String chat_media_uri;
    String chat_media_caption;
    String chat_media_sender;
    String chat_media_time;

    ChatMedia(String chat_media_uri, String chat_media_caption, String chat_media_sender, String chat_media_time){
        this.chat_media_uri = chat_media_uri;
        this.chat_media_caption = chat_media_caption;
        this.chat_media_sender = chat_media_sender;
        this.chat_media_time = chat_media_time;
    }
}