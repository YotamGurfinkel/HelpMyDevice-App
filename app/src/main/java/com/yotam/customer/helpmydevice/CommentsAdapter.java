package com.yotam.customer.helpmydevice;

import android.content.Context;
import android.graphics.Color;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

// Class to define adapter (List of comments > ListView)
public class CommentsAdapter extends ArrayAdapter<Comment> {

    private int resourceLayout; // int of the layout defined
    private Context mContext;

    public CommentsAdapter( Context context, int resource, List<Comment> objects) {
        super(context, resource, objects);
        this.resourceLayout = resource; // set the recourseLayout to the recourse passed
        this.mContext = context; // set the context to the context passed
    }


    @Override
    public View getView(int position,  View convertView,  ViewGroup parent) {
        View v = convertView; // get the current view

        if(v == null ){
            LayoutInflater vi;
            vi = LayoutInflater.from(mContext);
            v = vi.inflate(resourceLayout, null); // inflate the layout passed
        }

        Comment comment = getItem(position); // get the current item and put it into a Comment object

        if(comment != null /* check if the current comment isn't null */){
            TextView commenterName = v.findViewById(R.id.commenterName);
            ImageView commenterImg = v.findViewById(R.id.commenterAvatar);
            ImageView technicianIcon = v.findViewById(R.id.technicianIcon);
            TextView commentBody = v.findViewById(R.id.commentBody);
            TextView commentTime = v.findViewById(R.id.commentTime);

            // Load the current commenter image to the ImageView with Glide library
            if(comment.getImg() != null) {
                Glide.with(v)
                        .load(comment.getImg())
                        .apply(RequestOptions.circleCropTransform())
                        .into(commenterImg);
            }else{
                commenterImg.setImageDrawable(v.getResources().getDrawable(R.drawable.ic_menu_profile));
            }

            commentBody.setText(comment.getBody()); // set the current comment body into the TextView
            commenterName.setText(comment.getCurrentFullName()); // set the current commenter full name into the TextView
            if(comment.isTechnician() /* check if the current comment is by a technician */){
                technicianIcon.setVisibility(View.VISIBLE);
            }else{
                technicianIcon.setVisibility(View.GONE);
            }

            // Set the comment time to commentTime TextView
            Calendar cal = Calendar.getInstance(Locale.ENGLISH);
            cal.setTimeInMillis(comment.getTimeStamp());
            String date = DateFormat.format("dd-MM-yyyy HH:mm",cal).toString();
            commentTime.setText(date);
        }

        return v; // return the current view
    }
}
