package com.example.howzyou;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private ImageView mProfileImageview;
    private TextView mProfileName, mProfileStatus, mProfileFriendsCount;
    private Button mProfileSendReqBtn;
    private Button mProfileDeclineReqBtn;

    private DatabaseReference mUserDatabase;
    private DatabaseReference mFriendReqDatabase;
    private DatabaseReference mFriendDatabase;
    private DatabaseReference mNotificationDatabase;
    private DatabaseReference mRootRef;

    private ProgressDialog mProgressDialog;
    private FirebaseUser mCurrentUser;

    private String mCurrent_state ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        final String user_id = getIntent().getStringExtra("user_id");

        mRootRef = FirebaseDatabase.getInstance().getReference();

        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(user_id);
        mFriendReqDatabase = FirebaseDatabase.getInstance().getReference().child("Friend_req");
        mFriendDatabase = FirebaseDatabase.getInstance().getReference().child("Friends");
        mNotificationDatabase = FirebaseDatabase.getInstance().getReference().child("notifications");
        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();



        mProfileImageview = (ImageView) findViewById(R.id.profileImage);
        mProfileName = (TextView) findViewById(R.id.profileDisplayName);
        mProfileStatus = (TextView)findViewById(R.id.profileStatus);
        mProfileFriendsCount = (TextView) findViewById(R.id.profileFriendsCount);
        mProfileSendReqBtn = (Button) findViewById(R.id.profileSendReqBtn);
        mProfileDeclineReqBtn = (Button) findViewById(R.id.profileDeclineReqBtn);

        mCurrent_state = "not_friends";

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle("Loading Users Data");
        mProgressDialog.setMessage("Please wait while we load the data");
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.show();



        mUserDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String display_name = dataSnapshot.child("name").getValue().toString();
                String status = dataSnapshot.child("status").getValue().toString();
                String image = dataSnapshot.child("image").getValue().toString();

                mProfileName.setText(display_name);
                mProfileStatus.setText(status);

                Picasso.with(ProfileActivity.this).load(image).placeholder(R.drawable.manus1).into(mProfileImageview);

                mFriendReqDatabase.child(mCurrentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        if (dataSnapshot.hasChild(user_id)){

                            String req_type = dataSnapshot.child(user_id).child("request_type").getValue().toString();

                            if (req_type.equals("recieved")){

                                mCurrent_state = "request_recieved";
                                mProfileSendReqBtn.setText("Accept Friend Request");

                                mProfileDeclineReqBtn.setVisibility(View.VISIBLE);
                                mProfileDeclineReqBtn.setEnabled(true);

                            }else if (req_type.equals("sent")) {

                                mCurrent_state = "request_sent";
                                mProfileSendReqBtn.setText("Unsend Friend Request");

                                mProfileDeclineReqBtn.setVisibility(View.INVISIBLE);
                                mProfileDeclineReqBtn.setEnabled(false);

                            }
                            mProgressDialog.dismiss();
                        }else {

                                mFriendDatabase.child(mCurrentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.hasChild(user_id)){

                                            mCurrent_state = "friends";
                                            mProfileSendReqBtn.setText("Unfriend the User");

                                            mProfileDeclineReqBtn.setVisibility(View.INVISIBLE);
                                            mProfileDeclineReqBtn.setEnabled(false);
                                        }
                                        mProgressDialog.dismiss();
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                        mProgressDialog.dismiss();

                                    }
                                });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        mProfileSendReqBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mProfileSendReqBtn.setEnabled(false);

                //-------------------not friends state
                if (mCurrent_state.equals("not_friends")) {

                    DatabaseReference newNotificationRef = mRootRef.child("notifications").child(user_id).push();
                    String newNotificationId = newNotificationRef.getKey();

                    HashMap<String ,String > notificationData = new HashMap<>();
                    notificationData.put("from", mCurrentUser.getUid());
                    notificationData.put("type", "request");

                    Map requestMap = new HashMap();
                    requestMap.put("Friend_req/" + mCurrentUser.getUid() + "/" + user_id + "/request_type", "sent");
                    requestMap.put("Friend_req/" +user_id + "/" + mCurrentUser.getUid() + "/request_type", "recieved");
                    requestMap.put("notifications/"+ user_id + "/" + newNotificationId, notificationData);

                    mRootRef.updateChildren(requestMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {

                            if (databaseError != null){
                                Toast.makeText(ProfileActivity.this, "There was some error in sending request", Toast.LENGTH_LONG).show();
                            }
                            else{
                                mCurrent_state = "request_sent";
                                mProfileSendReqBtn.setText("Unsend Friend Request");
                            }

                            mProfileSendReqBtn.setEnabled(true);

                        }
                    });

                }

                if (mCurrent_state.equals("request_sent")) {
                    mFriendReqDatabase.child(mCurrentUser.getUid()).child(user_id).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {

                            mFriendReqDatabase.child(user_id).child(mCurrentUser.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {

                                    mProfileSendReqBtn.setEnabled(true);
                                    mCurrent_state = "not_friends";
                                    mProfileSendReqBtn.setText("Unsend Friend Request");

                                    mProfileDeclineReqBtn.setVisibility(View.INVISIBLE);
                                    mProfileDeclineReqBtn.setEnabled(false);
                                }
                            });
                        }
                    });

                }

                //---------------Request recieved state
                if (mCurrent_state.equals("request_recieved")) {

                    final String currentDate = DateFormat.getDateTimeInstance().format(new Date());

                    Map friendsMap = new HashMap();
                    friendsMap.put("Friends/" + mCurrentUser.getUid() + "/" + user_id + "/date", currentDate);
                    friendsMap.put("Friends/" +user_id + "/" + mCurrentUser.getUid() + "/date", currentDate);

                    friendsMap.put("Friend_req/" + mCurrentUser.getUid() + "/" + user_id, null);
                    friendsMap.put("Friend_req/" +user_id + "/" + mCurrentUser.getUid(), null);

                    mRootRef.updateChildren(friendsMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                            if (databaseError == null){
                                mProfileSendReqBtn.setEnabled(true);
                                mCurrent_state = "friends";
                                mProfileSendReqBtn.setText("Unfriend the User");

                                mProfileDeclineReqBtn.setVisibility(View.INVISIBLE);
                                mProfileDeclineReqBtn.setEnabled(false);
                            }
                            else{

                            }
                        }
                    });


//                    mFriendDatabase.child(mCurrentUser.getUid()).child(user_id).setValue(currentDate).addOnSuccessListener(new OnSuccessListener<Void>() {
//                        @Override
//                        public void onSuccess(Void aVoid) {
//
//                            mFriendDatabase.child(user_id).child(mCurrentUser.getUid()).setValue(currentDate).addOnSuccessListener(new OnSuccessListener<Void>() {
//                                @Override
//                                public void onSuccess(Void aVoid) {
//
//                                    mFriendReqDatabase.child(user_id).child(mCurrentUser.getUid()).removeValue()
//                                            .addOnSuccessListener(
//
//                                                    new OnSuccessListener<Void>() {
//                                                @Override
//                                                public void onSuccess(Void aVoid) {
//
//                                                    mProfileSendReqBtn.setEnabled(true);
//                                                    mCurrent_state = "friends";
//                                                    mProfileSendReqBtn.setText("Unfriend the User");
//
//                                                    mProfileDeclineReqBtn.setVisibility(View.VISIBLE);
//                                                    mProfileDeclineReqBtn.setEnabled(true);
//                                                }
//                                            });
//
//                                }
//                            });
//
//                        }
//                    });


                }
//                if (mCurrent_state.equals("friends")){
//
//                    mFriendDatabase.child(user_id).child(mCurrentUser.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
//                        @Override
//                        public void onSuccess(Void aVoid) {
//
//                            mCurrent_state = "not_friends";
//                            mProfileSendReqBtn.setText("Send Friend Request");
//                        }
//                    });
//
//
//                }

                //--------------Unfriend
                if (mCurrent_state.equals("friends")){

                    Map unfriendMap = new HashMap();
                    unfriendMap.put("Friends/" + mCurrentUser.getUid() + "/" + user_id, null);
                    unfriendMap.put("Friends/" + user_id + "/" + mCurrentUser.getUid(),null);

                    mRootRef.updateChildren(unfriendMap , new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {

                            if (databaseError == null){


                                mCurrent_state = "not_friends";
                                mProfileSendReqBtn.setText("Send Friend Request");

                                mProfileDeclineReqBtn.setVisibility(View.INVISIBLE);
                                mProfileDeclineReqBtn.setEnabled(false);
                                //Toast.makeText(ProfileActivity.this, "There was some error in sending request", Toast.LENGTH_LONG).show();
                            }else{

                                String error = databaseError.getMessage();
                                Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_LONG).show();

                            }
                            mProfileSendReqBtn.setEnabled(true);


                        }
                    });

                }


            }
        });



    }
}

