package com.example.howzyou;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
//import android.widget.Toolbar;
import androidx.appcompat.widget.Toolbar;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
//import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class UsersActivity extends AppCompatActivity {

    private Toolbar mToolbar;

    private RecyclerView mUsersList;

    private DatabaseReference mUsersDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);

        mToolbar = (Toolbar) findViewById(R.id.usersToolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("All Users");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("Users");
       // Toast.makeText(UsersActivity.this, ""+mUsersDatabase,Toast.LENGTH_LONG).show();
        Log.w(""+mUsersDatabase,"hello");

        mUsersList = (RecyclerView) findViewById(R.id.usersListID);
        mUsersList.setHasFixedSize(true);
        mUsersList.setLayoutManager(new LinearLayoutManager(this));

    }

    @Override
    protected void onStart() {
        super.onStart();

       FirebaseRecyclerAdapter firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Users, UsersViewHolder>
               (Users.class, R.layout.users_layout, UsersViewHolder.class, mUsersDatabase) {
           @Override
           protected void populateViewHolder(UsersViewHolder usersViewHolder, Users users, int i) {

               usersViewHolder.setName(users.getName());
               usersViewHolder.setUserStatus(users.getStatus());
               usersViewHolder.setUsersThumbnail(users.getThumb_image(), getApplicationContext());

               final String user_id = getRef(i).getKey();

               usersViewHolder.mView.setOnClickListener(new View.OnClickListener() {
                   @Override
                   public void onClick(View v) {

                       Intent profileIntent = new Intent(UsersActivity.this, ProfileActivity.class);
                       profileIntent.putExtra("user_id", user_id);
                       startActivity(profileIntent);

                   }
               });

           }
       };
        mUsersList.setAdapter(firebaseRecyclerAdapter);
    }

    public static class UsersViewHolder extends RecyclerView.ViewHolder{

        View mView;

        public UsersViewHolder(@NonNull View itemView) {
            super(itemView);

            mView = itemView;
        }

        public void setName(String name){

            TextView userNameView = mView.findViewById(R.id.userNameID);
            userNameView.setText(name);
        }

        public void setUserStatus(String status){

            TextView userStatusView = mView.findViewById(R.id.userStatusID);
            userStatusView.setText(status);

        }
        public void setUsersThumbnail(String thumb_image, Context ctx){

            CircleImageView userImageView = (CircleImageView) mView.findViewById(R.id.userSingleImageview);

            Picasso.with(ctx).load(thumb_image).placeholder(R.drawable.addimage).into(userImageView);
        }
    }
}
