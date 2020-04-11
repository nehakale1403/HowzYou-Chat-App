package com.example.howzyou;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.internal.FirebaseInstanceIdInternal;
//import android.widget.Toolbar;

public class LoginActivity extends AppCompatActivity {


    private Toolbar loginToolbar;
    private TextView loginEmail;
    private TextView loginPassword;
    private Button loginButton;
    private FirebaseAuth mAuth;
    private DatabaseReference mUserDatabase;


    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginToolbar = (Toolbar) findViewById(R.id.loginToolBar);
        loginEmail = (EditText) findViewById(R.id.loginEmailID);
        loginPassword = (EditText) findViewById(R.id.loginPasswordID);
        loginButton = (Button) findViewById(R.id.loginButtonID);

        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users");

        mProgressDialog = new ProgressDialog(this);

        mAuth = FirebaseAuth.getInstance();

        setSupportActionBar(loginToolbar);
        getSupportActionBar().setTitle("Login");

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String email = loginEmail.getText().toString();
                String pwd = loginPassword.getText().toString();

                if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(pwd)){

                    mProgressDialog.setTitle("Logging in");
                    mProgressDialog.setMessage("Please wait while we check your credentials");
                    mProgressDialog.setCanceledOnTouchOutside(false);
                    mProgressDialog.show();
                    loginUser(email,pwd);
                }

            }
        });

    }

    private void loginUser(String email, String pwd) {

        mAuth.signInWithEmailAndPassword(email,pwd).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {

                if (task.isSuccessful()){
                    mProgressDialog.dismiss();

                    String currentUserID = mAuth.getCurrentUser().getUid();

                    String deviceToken = FirebaseInstanceId.getInstance().getToken();

                    mUserDatabase.child(currentUserID).child("device_token").setValue(deviceToken).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {

                            Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
                            mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(mainIntent);
                            finish();


                        }
                    });


                }else{
                    mProgressDialog.hide();
                    Toast.makeText(LoginActivity.this, "Cannot log in. Please re-check the credentials", Toast.LENGTH_LONG).show();

                }
            }
        });



    }
}
