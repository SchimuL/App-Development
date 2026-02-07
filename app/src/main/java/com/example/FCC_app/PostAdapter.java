package com.example.FCC_app;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    public interface OnPostClickListener {
        void onPostClick(Post post);
    }

    private final List<Post> postList;
    private final String currentPlayerTag;
    private final OnPostClickListener listener;

    public PostAdapter(List<Post> postList, String currentPlayerTag, OnPostClickListener listener) {
        this.postList = postList;
        this.currentPlayerTag = currentPlayerTag;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);
        holder.bind(post, currentPlayerTag, listener);
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        private final TextView subjectTextView;
        private final TextView dateTextView;
        private final ImageView unreadIndicator;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            subjectTextView = itemView.findViewById(R.id.post_subject);
            dateTextView = itemView.findViewById(R.id.post_date);
            unreadIndicator = itemView.findViewById(R.id.unread_indicator);
        }

        void bind(final Post post, final String currentPlayerTag, final OnPostClickListener listener) {
            subjectTextView.setText(post.getSubject());

            SimpleDateFormat sdf = new SimpleDateFormat("dd. MMM yyyy", Locale.GERMAN);
            dateTextView.setText(sdf.format(post.getTimestamp()));

            // Check if the post has been read by the current player
            if (post.getReadBy() != null && post.getReadBy().contains(currentPlayerTag)) {
                unreadIndicator.setVisibility(View.GONE);
                subjectTextView.setTypeface(null, Typeface.NORMAL);
            } else {
                unreadIndicator.setVisibility(View.VISIBLE);
                subjectTextView.setTypeface(null, Typeface.BOLD);
            }

            itemView.setOnClickListener(v -> listener.onPostClick(post));
        }
    }
}
