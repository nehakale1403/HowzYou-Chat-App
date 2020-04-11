package com.example.howzyou;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
//import android.widget.Toolbar;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    private String mChatUser;
    private Toolbar mChatToolbar;

    private TextView mTitleView;
    private TextView mLastSeenView;
    private CircleImageView mProfileImage;
    private FirebaseAuth mAuth;
    private String mCurrentUserID;

    private ImageButton mChatAddButton;
    private ImageButton mChatSendButton;
    private EditText mChatMessageView;

    private RecyclerView mMessagesList;

    private SwipeRefreshLayout refreshLayout;

    private final List<Messages> messageList = new ArrayList<>();
    private LinearLayoutManager mLinearLayout;

    private MessageAdapter mAdapter;
    private DatabaseReference mRootRef;

    private static final int GALLERY_PICK = 1;

    private static final int TOTAL_ITEMS_TO_LOAD = 10;
    private int mCurrentPage = 1;

    //new soln
    private int itemPosition = 0;
    private String mLastKey = "";
    private String mPrevKey = "";

    private StorageReference mImageStorage;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mChatToolbar = (Toolbar) findViewById(R.id.chatAppBar);
        setSupportActionBar(mChatToolbar);

        ActionBar actionBar= getSupportActionBar();

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);

        mRootRef = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        mCurrentUserID = mAuth.getCurrentUser().getUid();


        mChatUser = getIntent().getStringExtra("user_id");
        String userName = getIntent().getStringExtra("user_name");
        //getSupportActionBar().setTitle(userName);

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View action_bar_view = inflater.inflate(R.layout.chat_custom_bar, null);

        actionBar.setCustomView(action_bar_view);

        mTitleView = (TextView) findViewById(R.id.displayNameCustomview);
        mLastSeenView = (TextView) findViewById(R.id.lastSeenCustomView);
        mProfileImage = (CircleImageView) findViewById(R.id.customBarImage);

        mChatAddButton = (ImageButton) findViewById(R.id.chatAddButton);
        mChatSendButton = (ImageButton) findViewById(R.id.chatSendButton);
        mChatMessageView = (EditText) findViewById(R.id.chatMessageView);

        mAdapter = new MessageAdapter(messageList);

        mMessagesList = (RecyclerView) findViewById(R.id.messages_list);
        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeLayout);


        mLinearLayout = new LinearLayoutManager(this);
        mMessagesList.setHasFixedSize(true);
        mMessagesList.setLayoutManager(mLinearLayout);

        mMessagesList.setAdapter(mAdapter);
        mImageStorage = FirebaseStorage.getInstance().getReference();

        mRootRef.child("Chat").child(mCurrentUserID).child(mChatUser).child("seen").setValue(true);

        loadMessages();

        mTitleView.setText(userName);

        mRootRef.child("Users").child(mChatUser).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange( DataSnapshot dataSnapshot) {

                String online = dataSnapshot.child("online").getValue().toString();
                String image = dataSnapshot.child("image").getValue().toString();

                if (online.equals("true")){
                    mLastSeenView.setText("online");

                }else {

                    GetTimeAgo getTimeAgo = new GetTimeAgo();
                    long lastTime = Long.parseLong(online);
                    String lastSeenTime = getTimeAgo.getTimeAgo(lastTime,getApplicationContext());

                    mLastSeenView.setText(lastSeenTime);
                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        mRootRef.child("Chat").child(mCurrentUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (!dataSnapshot.hasChild(mChatUser)){

                    Map chatAddMap = new HashMap();
                    chatAddMap.put("seen", false);
                    chatAddMap.put("timestamp", ServerValue.TIMESTAMP);

                    Map chatUserMap = new HashMap();
                    chatUserMap.put("Chat/" + mCurrentUserID +"/"+mChatUser, chatAddMap);
                    chatUserMap.put("Chat/" + mChatUser +"/" + mCurrentUserID, chatAddMap);



                    mRootRef.updateChildren(chatUserMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete( DatabaseError databaseError,  DatabaseReference databaseReference) {

                            if (databaseError!=null){

                                Log.d("CHAT_LOG",databaseError.getMessage().toString());

                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mChatSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                sendMessage();
            }
        });

        mChatAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent galleryIntent = new Intent();
                galleryIntent.setType("image/*");
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(Intent.createChooser(galleryIntent, "SELECT IMAGE"),GALLERY_PICK);
            }
        });

        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
             @Override
             public void onRefresh() {

                 mCurrentPage++;

                 itemPosition = 0;
                // messageList.clear();
                loadMoreMessages();

             }
          });


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_PICK && resultCode ==RESULT_OK){

            Uri imageUri = data.getData();

            final String currentUserRef = "messages/" + mCurrentUserID +"/" + mChatUser;
            final String chatUserRef = "messages/" + mChatUser + "/" + mCurrentUserID;

            DatabaseReference userMsgPush = mRootRef.child("messages").child(mCurrentUserID).child(mChatUser).push();

            final String pushID = userMsgPush.getKey();

            StorageReference filepath = mImageStorage.child("message_images").child(pushID + ".jpg");

            filepath.putFile(imageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {

                    if (task.isSuccessful()){

                        String downloadUrl = task.getResult().getMetadata().getReference().getDownloadUrl().toString();

                        Map messageMap = new HashMap();
                        messageMap.put("message", downloadUrl);
                        messageMap.put("seen",false);
                        messageMap.put("type","image");
                        messageMap.put("time", ServerValue.TIMESTAMP);
                        messageMap.put("from",mCurrentUserID);

                        Map messageUserMap = new HashMap();
                        messageUserMap.put(currentUserRef + "/" + pushID, messageMap);
                        messageUserMap.put(chatUserRef + "/" + pushID, messageMap);

                        //Log.d( mCurrentUserID +"!!!!"+ mChatUser, "MSG");


                        mChatMessageView.setText("");

                        mRootRef.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                                if (databaseError != null){
                                    Log.d("CHAT_LOG", databaseError.getMessage().toString());
                                }
                            }
                        });
                    }
                }
            });
        }
    }

    private void loadMoreMessages() {

        DatabaseReference messageRef = mRootRef.child("messages").child(mCurrentUserID).child(mChatUser);

        Query messageQuery = messageRef.orderByKey().endAt(mLastKey).limitToLast(10);

        messageQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot,  String s) {

                Messages messages = dataSnapshot.getValue(Messages.class);
                String messageKey = dataSnapshot.getKey();

                if (!mPrevKey.equals(messageKey)){

                    messageList.add(itemPosition++, messages);
                }else{

                    mPrevKey = mLastKey;
                }

                if (itemPosition == 1){


                    mLastKey = messageKey;
                }
                mAdapter.notifyDataSetChanged();

                //mMessagesList.scrollToPosition(messageList.size()-1);

                refreshLayout.setRefreshing(false);
                mLinearLayout.scrollToPositionWithOffset(10,0);

            }

            @Override
            public void onChildChanged( DataSnapshot dataSnapshot,  String s) {

            }

            @Override
            public void onChildRemoved( DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot,  String s) {

            }

            @Override
            public void onCancelled( DatabaseError databaseError) {

            }
        });


    }

    private void loadMessages() {

        DatabaseReference messageRef = mRootRef.child("messages").child(mCurrentUserID).child(mChatUser);

        Query messageQuery = messageRef.limitToLast(mCurrentPage * TOTAL_ITEMS_TO_LOAD);

        messageQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot,  String s) {

                Messages messages = dataSnapshot.getValue(Messages.class);

                itemPosition++;
                if (itemPosition == 1){

                    String messageKey = dataSnapshot.getKey();
                    mLastKey = messageKey;
                    mPrevKey = messageKey;
                }

                messageList.add(messages);
                mAdapter.notifyDataSetChanged();

                mMessagesList.scrollToPosition(messageList.size()-1);

                refreshLayout.setRefreshing(false);

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved( DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved( DataSnapshot dataSnapshot,  String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void sendMessage() {

        String message = mChatMessageView.getText().toString();

        if (!TextUtils.isEmpty(message)){

            String currentUserRef = "messages/" + mCurrentUserID+ "/" + mChatUser;
            String chatUserRef = "messages/" + mChatUser + "/" + mCurrentUserID;

            DatabaseReference userMsgPush = mRootRef.child("messages").child(mCurrentUserID).child(mChatUser).push();

            String push_id = userMsgPush.getKey();

            Map messageMap = new HashMap();
            messageMap.put( "message", message);
            messageMap.put("seen", false);
            messageMap.put("type", "text");
            messageMap.put("time", ServerValue.TIMESTAMP);
            messageMap.put("from", mCurrentUserID);

            Map messageUserMap = new HashMap();
            messageUserMap.put(currentUserRef + "/" + push_id, messageMap);
            messageUserMap.put(chatUserRef + "/" + push_id, messageMap);

            mChatMessageView.setText("");

            mRootRef.child("Chat").child(mCurrentUserID).child(mChatUser).child("seen").setValue(true);
            mRootRef.child("Chat").child(mCurrentUserID).child(mChatUser).child("timestamp").setValue(ServerValue.TIMESTAMP);

            mRootRef.child("Chat").child(mChatUser).child(mCurrentUserID).child("seen").setValue(false);
            mRootRef.child("Chat").child(mChatUser).child(mCurrentUserID).child("timestamp").setValue(ServerValue.TIMESTAMP);

            mRootRef.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete( DatabaseError databaseError, DatabaseReference databaseReference) {
                if (databaseError!=null){

                    Log.d("CHAT_LOG", databaseError.getMessage().toString());
                }
                }
            });
        }
    }
}
