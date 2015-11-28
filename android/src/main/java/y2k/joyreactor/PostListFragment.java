package y2k.joyreactor;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.ocpsoft.prettytime.PrettyTime;
import y2k.joyreactor.common.ItemDividerDecoration;
import y2k.joyreactor.platform.ImageRequest;
import y2k.joyreactor.presenters.PostListPresenter;

import java.util.List;

/**
 * Created by y2k on 9/26/15.
 */
public class PostListFragment extends Fragment {

    PostListPresenter presenter;
    PostAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_posts, container, false);

        view.findViewById(R.id.error).setVisibility(View.GONE);

        RecyclerView list = (RecyclerView) view.findViewById(R.id.list);
        list.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        list.setAdapter(adapter = new PostAdapter());
        list.addItemDecoration(new ItemDividerDecoration(list));

        presenter = new PostListPresenter(new PostListPresenter.View() {

            @Override
            public void setBusy(boolean isBusy) {
                // TODO:
            }

            @Override
            public void reloadPosts(List<Post> posts, Integer divider) {
                adapter.reloadData(posts);
            }

            @Override
            public void setHasNewPosts(boolean hasNewPosts) {
                ((ReloadButton) getView().findViewById(R.id.apply)).setVisibility(hasNewPosts);
            }
        });

        view.findViewById(R.id.apply).setOnClickListener(v -> presenter.applyNew());
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.activate();
    }

    @Override
    public void onPause() {
        super.onPause();
        presenter.deactivate();
    }

    class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

        private PrettyTime prettyTime = new PrettyTime();
        private List<Post> posts;

        @Override
        public PostViewHolder onCreateViewHolder(ViewGroup viewGroup, int itemType) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.item_feed, viewGroup, false);
            return new PostViewHolder(view);
        }

        @Override
        public void onBindViewHolder(PostViewHolder h, int position) {
            Post i = posts.get(position);

            Image image = i.image;
            if (image == null) {
                h.imagePanel.setVisibility(View.GONE);
            } else {
                h.imagePanel.setVisibility(View.VISIBLE);
                h.imagePanel.setAspect(image.getAspect(0.5f));

                new ImageRequest()
                        .setUrl(i.image)
                        .setSize(200, (int) (200 / image.getAspect(0.5f)))
                        .to(h.image, h.image::setImageBitmap);
            }

            h.userImage.setImage(i.getUserImage().toImage());
            h.videoMark.setVisibility(image != null && image.isAnimated() ? View.VISIBLE : View.GONE);

            h.commentCount.setText("" + i.commentCount);
            h.time.setText(prettyTime.format(i.created));
        }

        @Override
        public int getItemCount() {
            return posts == null ? 0 : posts.size();
        }

        public void reloadData(List<Post> posts) {
            this.posts = posts;
            notifyDataSetChanged();
        }

        class PostViewHolder extends RecyclerView.ViewHolder {

            FixedAspectPanel imagePanel;
            WebImageView image;
            WebImageView userImage;
            View videoMark;
            TextView commentCount;
            TextView time;

            public PostViewHolder(View view) {
                super(view);

                image = (WebImageView) view.findViewById(R.id.image);
                imagePanel = (FixedAspectPanel) view.findViewById(R.id.imagePanel);
                userImage = (WebImageView) view.findViewById(R.id.userImage);
                videoMark = view.findViewById(R.id.videoMark);
                commentCount = (TextView) view.findViewById(R.id.commentCount);
                time = (TextView) view.findViewById(R.id.time);

                view.findViewById(R.id.card).setOnClickListener(
                        v -> presenter.postClicked(posts.get(getAdapterPosition())));
                view.findViewById(R.id.videoMark).setOnClickListener(
                        v -> presenter.playClicked(posts.get(getAdapterPosition())));
            }
        }
    }
}