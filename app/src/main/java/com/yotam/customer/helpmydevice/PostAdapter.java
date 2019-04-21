package com.yotam.customer.helpmydevice;

import android.content.Context;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import static com.yotam.customer.helpmydevice.Constants.currentPostFollowing;

public class PostAdapter extends ArrayAdapter<Post> implements Filterable {

    private List<Post> originalData = null;
    private List<Post> filteredData = null;
    private PostFilter mFilter = new PostFilter();
    private int resourceLayout;
    private Context mContext;

    public PostAdapter( Context context, int resource, List<Post> objects) {
        super(context, resource, objects);
        this.resourceLayout = resource;
        this.mContext = context;
        this.filteredData = objects;
        this.originalData = objects;
    }

    @Override
    public int getCount() {
        return filteredData.size();
    }

    @Override
    public Post getItem(int position) {
        return filteredData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView,  ViewGroup parent) {
        Post post;
        View v = convertView;

        if(v == null){
            LayoutInflater vi;
            vi = LayoutInflater.from(mContext);
            v = vi.inflate(resourceLayout, null);
        }

        post = filteredData.get(position);


        if(post != null){
            TextView postName = v.findViewById(R.id.post_title);
            TextView postTime = v.findViewById(R.id.post_timestamp);
            TextView timeType = v.findViewById(R.id.timeType);
            ImageView postImg = v.findViewById(R.id.post_image);
            ImageView solvedMark = v.findViewById(R.id.solved_mark);
            TextView isFollowing = v.findViewById(R.id.isFollowing);

            Calendar cal = Calendar.getInstance(Locale.ENGLISH);
            cal.setTimeInMillis(post.getTimestamp());
            String date = DateFormat.format("dd-MM-yyyy HH:mm",cal).toString();

            postName.setText(post.getTitle());
            postTime.setText(date);
            solvedMark.setVisibility(View.GONE);

            if(currentPostFollowing != null && currentPostFollowing.equals(post.getPostId())){
                isFollowing.setVisibility(View.VISIBLE);
            }else{
                isFollowing.setVisibility(View.INVISIBLE);
            }
            if(post.isSolved()){
                timeType.setText("Solved On:");
                solvedMark.setVisibility(View.VISIBLE);
            }
            else if(post.isEdited()){
                timeType.setText("Edited On:");
            }
            else{
                timeType.setText("Created On:");
            }
            Glide.with(v).load(post.getImg()).into(postImg);
        }

        return v;
    }

    @Override
    public Filter getFilter(){
        return mFilter;
    }

    private class PostFilter extends Filter{
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            String filterString = constraint.toString().toLowerCase();

            FilterResults results = new FilterResults();

            final List<Post> list = originalData;

            int count = list.size();
            final ArrayList<Post> newList = new ArrayList<>(count);

            Post queryPost;

            for (int i = 0; i < count; i++) {
                queryPost = list.get(i);
                if(queryPost.getTitle().toLowerCase().contains(filterString) || queryPost.getDescription().toLowerCase().contains(filterString)){
                    newList.add(queryPost);
                }
            }

            results.values = newList;
            results.count = newList.size();

            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            filteredData = (ArrayList<Post>) results.values;
            notifyDataSetChanged();
        }
    }
}
