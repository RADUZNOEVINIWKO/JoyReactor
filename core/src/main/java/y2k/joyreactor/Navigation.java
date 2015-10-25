package y2k.joyreactor;

/**
 * Created by y2k on 02/10/15.
 */
public abstract class Navigation {

    public static Navigation getInstance() {
        return Platform.Instance.getNavigator();
    }

    public abstract void switchProfileToLogin();

    public abstract void switchLoginToProfile();

    public abstract void closeCreateComment();

    public abstract void closeAddTag();

    public abstract void openPost(Post post);

    public abstract Post getArgumentPost();

    public abstract void openBrowser(String url);

    public abstract void openVideo(Post post);

    public abstract void openImageView(Post post);
}