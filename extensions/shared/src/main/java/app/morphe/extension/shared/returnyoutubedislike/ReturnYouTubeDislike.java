package app.morphe.extension.shared.returnyoutubedislike;

public class ReturnYouTubeDislike {

    public enum Vote {
        LIKE("like/like", 1),
        DISLIKE("like/dislike", -1),
        LIKE_REMOVE("like/removelike", 0);

        public final String endpoint;
        public final int value;

        Vote(String endpoint, int value) {
            this.endpoint = endpoint;
            this.value = value;
        }
    }

}
