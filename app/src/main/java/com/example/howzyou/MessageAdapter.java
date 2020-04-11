package com.example.howzyou;

import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Messages> mMessageList;
   // private FirebaseAuth mAuth;
    private DatabaseReference mUserDatabase;


    public MessageAdapter(List<Messages> mMessageList){
        this.mMessageList = mMessageList;

    }

    @Override
    public MessageViewHolder onCreateViewHolder( ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_single_layout,parent,false);

        return new MessageViewHolder(v);

    }

    public class MessageViewHolder extends RecyclerView.ViewHolder {

        public TextView messageText;
        public CircleImageView profileImage;
        public ImageView messageImage;

        public MessageViewHolder( View itemView) {
            super(itemView);

            messageText = (TextView) itemView.findViewById(R.id.message_text_layout);
            profileImage = (CircleImageView) itemView.findViewById(R.id.message_profile_layout);
            messageImage = (ImageView) itemView.findViewById(R.id.message_image_layout);

        }
    }

    @Override
    public void onBindViewHolder( final MessageViewHolder holder, int position) {

       // String currentUserid = mAuth.getCurrentUser().getUid();

        Messages c  = mMessageList.get(position);
        Log.w("HERE" + c, "MSG");
        holder.messageText.setText(c.getMessage());


        String from_user = c.getFrom();
        String msg_type = c.getType();
        //Toast.makeText( ,""+from_user, Toast.LENGTH_SHORT).show();
        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(from_user);


        mUserDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                //String name =dataSnapshot.child("name").getValue().toString();
                String image = dataSnapshot.child("thumb_image").getValue().toString();



                Picasso.with(holder.profileImage.getContext()).load(image).placeholder(R.drawable.manus1)
                        .into(holder.profileImage);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        if (msg_type.equals("text")){

            holder.messageText.setText(c.getMessage());
            holder.messageImage.setVisibility(View.INVISIBLE);

            holder.messageText.setBackgroundResource(R.drawable.message_text_background);
            holder.messageText.setTextColor(Color.WHITE);

        }else {

            holder.messageText.setVisibility(View.INVISIBLE);

            Picasso.with(holder.profileImage.getContext()).load(c.getMessage()).placeholder(R.drawable.manus1)
                    .into(holder.messageImage);


        }
//
//        if (from_user.equals(currentUserid)){
//
//            holder.messageText.setBackgroundColor(Color.WHITE);
//            holder.messageText.setTextColor(Color.BLACK);
//
//        }else{
//
//            holder.messageText.setBackgroundResource(R.drawable.message_text_background);
//            holder.messageText.setTextColor(Color.WHITE);
//
//        }
//        holder.messageText.setText(c.getMessage());

    }

    @Override
    public int getItemCount() {
        return mMessageList.size();
    }


}
