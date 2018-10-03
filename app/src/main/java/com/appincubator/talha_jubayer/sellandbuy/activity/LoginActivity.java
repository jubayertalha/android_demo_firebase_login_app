package com.appincubator.talha_jubayer.sellandbuy.activity;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.appincubator.talha_jubayer.sellandbuy.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import static com.google.android.gms.auth.api.signin.GoogleSignIn.getClient;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    private static int RC_SIGN_IN = 0;
    GoogleSignInOptions gso;
    GoogleSignInClient mGoogleSignInClient;

    ProgressBar progressBar;
    EditText et_userName;
    Button button;
    FirebaseUser firebaseUser;

    private int RC_BUTTON = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        progressBar = findViewById(R.id.progressBar);
        et_userName = findViewById(R.id.et_userName);
        button = findViewById(R.id.button);

        et_userName.setVisibility(View.GONE);
        button.setVisibility(View.GONE);


        // Configure Google Sign In
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = getClient(this, gso);


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (RC_BUTTON == 0){
                    signIn();
                }else {
                    createAccount();
                }
            }
        });


    }

    private void updateUI(FirebaseUser user){
        if(user!=null){
            firebaseUser = user;
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("users").child("metadata").child(user.getUid());
            reference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    boolean exist = dataSnapshot.exists();
                    if (exist){
                        String userName = (String) dataSnapshot.getValue().toString();
                        Intent intent = new Intent(LoginActivity.this,MainActivity.class);
                        intent.putExtra("userName",userName);
                        startActivity(intent);
                        finish();
                    }else {
                        et_userName.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                        button.setVisibility(View.VISIBLE);
                        button.setText("Sign Up");
                        RC_BUTTON = 1;
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }else {
            et_userName.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            button.setVisibility(View.VISIBLE);
            button.setText("Sign In");
            RC_BUTTON = 0;
        }
    }

    private void createAccount(){
        final String userName = et_userName.getText().toString().trim();
        boolean valid = true;
        for(int i = 0; i < userName.length(); i++){
            char c = userName.charAt(i);
            if(!isLetterOrDigit(c)){
                valid = false;
                Toast.makeText(LoginActivity.this,"User name is not Valid. Use \"a-z\",\"A-Z\" and \"0-9\".",Toast.LENGTH_SHORT).show();
                break;
            }
        }
        if (valid){
            et_userName.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            button.setVisibility(View.GONE);
            RC_BUTTON = 1;
            final DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("users").child("details").child(userName);
            reference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    boolean exist = dataSnapshot.exists();
                    if (!exist){
                        reference.child("displayName").setValue(firebaseUser.getDisplayName()).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("users").child("metadata").child(firebaseUser.getUid());
                                reference.setValue(userName).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        updateUI(firebaseUser);
                                    }
                                });
                            }
                        });
                    }else {
                        Toast.makeText(LoginActivity.this,"User name is already taken. Try another.",Toast.LENGTH_SHORT).show();
                        et_userName.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                        button.setVisibility(View.VISIBLE);
                        button.setText("Sign Up");
                        RC_BUTTON = 1;
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }

    private static boolean isLetterOrDigit(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                (c >= '0' && c <= '9');
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);

                et_userName.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                button.setVisibility(View.GONE);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                //Log.w(TAG, "Google sign in failed", e);
                // ...
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        //Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            //Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            mGoogleSignInClient.signOut();
                            updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            //Log.w(TAG, "signInWithCredential:failure", task.getException());
                            //Snackbar.make(findViewById(R.id.main_layout), "Authentication Failed.", Snackbar.LENGTH_SHORT).show();
                            updateUI(null);
                        }

                        // ...
                    }
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
    }
}
