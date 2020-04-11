package com.example.howzyou;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.widget.Toolbar;
import android.widget.Toast;
//import android.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    private EditText regDisplayName;
    private EditText regEmail;
    private EditText regPassword;
    private Button regCreateAct;
    private ProgressDialog mProgressDialog;
    private Toolbar registerToolBar;

    private FirebaseAuth mAuth;

    private DatabaseReference mDatabase;
   // private DatabaseReference mUserDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        regDisplayName = (EditText) findViewById(R.id.registerDisplayName);
        regEmail = (EditText) findViewById(R.id.registerEmailid);
        regPassword = (EditText) findViewById(R.id.registerPassword);
        regCreateAct = (Button) findViewById(R.id.createActButton);
       // mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users");

        registerToolBar = (Toolbar) findViewById(R.id.registerToolbar);
        setSupportActionBar(registerToolBar);
        getSupportActionBar().setTitle("Create Account");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mAuth = FirebaseAuth.getInstance();

        mProgressDialog = new ProgressDialog(this);

        regCreateAct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String display_name = regDisplayName.getText().toString();
                String email = regEmail.getText().toString();
                String pwd = regPassword.getText().toString();

                if (!TextUtils.isEmpty(display_name) && !TextUtils.isEmpty(email) && !TextUtils.isEmpty(pwd)){

                    mProgressDialog.setTitle("Registering User");
                    mProgressDialog.setMessage("Please wait while we create your account");
                    mProgressDialog.setCanceledOnTouchOutside(false);
                     mProgressDialog.show();
                    registerUser(display_name,email,pwd);

                }else{
                    Toast.makeText(RegisterActivity.this,"Please fill appropriate details", Toast.LENGTH_LONG).show();
                }


            }
        });
    }

    private void registerUser(final String display_name, String email, String pwd) {

        mAuth.createUserWithEmailAndPassword(email, pwd)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information

                            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                            String uid = currentUser.getUid();

                            mDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(uid);

                            String device_token = FirebaseInstanceId.getInstance().getToken();

                                    HashMap<String, String> userMap = new HashMap<>();
                                    userMap.put("name", display_name);
                                    userMap.put("status", "Hey there! I am using HowzYouu chat app");
                                    userMap.put("image", "default");
                                    userMap.put("thumb_image" , "default");
                                    userMap.put("device_token", device_token);


                                    mDatabase.setValue(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {

                                            if (task.isSuccessful()){

                                                mProgressDialog.dismiss();
                                                Intent mainIntent = new Intent(RegisterActivity.this, MainActivity.class);
                                                mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                startActivity(mainIntent);
                                                finish();
                                            }
                                        }
                                    });
                        } else {
                            // If sign in fail s, display a message to the user.

                            mProgressDialog.hide();

                            Toast.makeText(RegisterActivity.this, "Authentication failed.",
                                    Toast.LENGTH_LONG).show();
                        }


                }
                });
    }
}
