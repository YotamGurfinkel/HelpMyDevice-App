package com.yotam.customer.helpmydevice;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static com.yotam.customer.helpmydevice.Constants.EXISTING_POST_REQUEST_CODE;
import static com.yotam.customer.helpmydevice.Constants.NEW_POST_REQUEST_CODE;
import static com.yotam.customer.helpmydevice.Constants.user;

public class PostsFragment extends Fragment implements SearchView.OnQueryTextListener {

    private SearchView searchView;
    private ListView mPostsList;
    List<Post> postList;
    PostAdapter adapter;
    private DatabaseReference reference;
    private ProgressDialog progressDialog;
    private static Post postForEdit;
    private String[] userDetails;
    private RelativeLayout noPosts;
    private static final String TAG = "PostsFragment";



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        if(user == null){
            user = FirebaseAuth.getInstance().getCurrentUser();
            CustomerActivity.setDetails(getActivity());
        }
        userDetails = user.getDisplayName().split(Pattern.quote("|"),4);
        View view = inflater.inflate(R.layout.fragment_posts, container, false);
        reference = FirebaseDatabase.getInstance().getReference("posts");
        mPostsList = view.findViewById(R.id.list_post);
        postList = new ArrayList<>();
        adapter = new PostAdapter(getContext(), R.layout.row_post, postList);
        progressDialog = new ProgressDialog(getContext());
        noPosts = view.findViewById(R.id.noPosts);
        progressDialog.setMessage("Loading Posts...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        reference.orderByChild("timestamp").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                postList.clear();
                for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                    postList.add(childSnapshot.getValue(Post.class));
                }
                Log.d(TAG, "onDataChange: " + postList.size());
                Collections.reverse(postList);
                if (searchView != null)
                    adapter.getFilter().filter(searchView.getQuery());
                if(!postList.isEmpty()) {
                    mPostsList.setAdapter(adapter);
                    noPosts.setVisibility(View.GONE);
                }else{
                    noPosts.setVisibility(View.VISIBLE);
                }
                progressDialog.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        mPostsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getContext(), PostView.class);
                Post currentClicked = (Post) parent.getItemAtPosition(position);
                intent.putExtra("postTitle", currentClicked.getTitle());
                intent.putExtra("postDescription", currentClicked.getDescription());
                intent.putExtra("postTime", currentClicked.getTimestamp());
                intent.putExtra("userId", currentClicked.getUserId());
                intent.putExtra("postId", currentClicked.getPostId());
                intent.putExtra("postImg", currentClicked.getImg());
                intent.putExtra("postEmail", currentClicked.getEmail());
                intent.putExtra("postSolved", currentClicked.isSolved());
                intent.putExtra("postEdited", currentClicked.isEdited());
                startActivity(intent);
            }
        });

        if (userDetails[3].equals("User")) {
            mPostsList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                    Log.d(TAG, "onItemLongClick: userId is " + user.getUid());
                    if (!((Post) parent.getItemAtPosition(position)).getUserId().equals(user.getUid())) {
                        Toast.makeText(getContext(), "You Can Only Edit Your Posts", Toast.LENGTH_SHORT).show();
                    } else {
                        postForEdit = (Post) parent.getItemAtPosition(position);
                        if (PhotoHelper.verifyPermissions(getContext() , EXISTING_POST_REQUEST_CODE)) {
                            Log.d(TAG, "onItemLongClick: permissions granted");
                            AddPostDialog editDialog = new AddPostDialog();
                            editDialog.setPermissionsGranted(true);
                            editDialog.setPost(postForEdit);
                            editDialog.show(getFragmentManager(), getString(R.string.dialog_post));
                        }
                    }
                    return true;
                }
            });
        }

        return view;
    }

    public static Post getEditingPost(){
        return postForEdit;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle("Posts");
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

    }

    @Override
    public void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem searchItem = menu.findItem(R.id.action_search);
        MenuItem addPost = menu.findItem(R.id.action_addPost);
        if(userDetails[3].equals("User")) {
            addPost.setVisible(true);
        }
        addPost.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if(PhotoHelper.verifyPermissions(getContext(), NEW_POST_REQUEST_CODE)) {
                    AddPostDialog dialog = new AddPostDialog();
                    dialog.setPermissionsGranted(true);
                    dialog.show(getFragmentManager(), getString(R.string.dialog_post));
                }
                return true;
            }
        });
        searchItem.setVisible(true);
        searchView = (SearchView) searchItem.getActionView();
        searchView.setSubmitButtonEnabled(false);
        searchView.setOnQueryTextListener(this);
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        ((PostAdapter) mPostsList.getAdapter()).getFilter().filter(newText);
        return true;
    }


}
