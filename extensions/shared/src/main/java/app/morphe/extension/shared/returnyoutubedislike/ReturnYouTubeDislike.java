package app.morphe.extension.shared.returnyoutubedislike;

public class ReturnYouTubeDislike {

    public enum Vote {
        LIKE(1),
        DISLIKE(-1),
        LIKE_REMOVE(0);

        public final int value;

        Vote(int value) {
            this.value = value;
        }
    }

}