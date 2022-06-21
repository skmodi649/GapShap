package com.example.gapshap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.auth.api.signin.internal.Storage;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    public static final String ANONYMOUS = "anonymous";

    public static final int MESSAGE_LENGTH_LIMIT = 1000;

    public static final int RC_SIGN_IN = 1;

    private static final int RC_PHOTO_PICKER =  2;

    private ListView mMessageListView;
    private MessageAdapter mMessageAdapter;
    private ProgressBar mProgressBar;
    private ImageButton mPhotoPickerButton;
    private EditText mMessageEditText;
    private Button mSendButton;

    private String mUsername;

    // Now integrating the functionality of Realtime database
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mMessageDatabaseReference;

    // Creating instance for Firebase storage and storage reference
    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;

    // Child event listener for reading data from the realtime database
    private ChildEventListener childEventListener;

    // Creating instance for firebase auth and auth state listener
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUsername = ANONYMOUS;

        // Initializing firebase components
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();



        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mMessageDatabaseReference = mFirebaseDatabase.getReference().child("messages");

        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference().child("chat_photos");

        // Initialize references to views
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mMessageListView = (ListView) findViewById(R.id.messageListView);
        mPhotoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mSendButton = (Button) findViewById(R.id.sendButton);

        // Initializing the message list view and its adapter

        List<ModelClass> modelClassList = new ArrayList<>();
        mMessageAdapter = new MessageAdapter(this, R.layout.item_message, modelClassList);
        mMessageListView.setAdapter(mMessageAdapter);


        // Initializing the progress bar

        mProgressBar.setVisibility(ProgressBar.INVISIBLE);

        // Image picker button displays an image picker to upload a image for a message
        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO : Fire an intent to show a image picker
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);
            }
        });

        // Enable send button only when there is text to send

        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Nothing to do over here
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.toString().trim().length() > 0)
                    mSendButton.setEnabled(true);
                else
                    mSendButton.setEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Nothing to do here as well
            }
        });

        // Limiting the message length to 1000 words only
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MESSAGE_LENGTH_LIMIT)});

        // Send button sends the message and clears the editText
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Creating the object for the Model class that contains name, message and photo url
                ModelClass modelClass = new ModelClass(mUsername, mMessageEditText.getText().toString(), null);
                // We have kept photo url to be null as of now
                // Now storing the data to database, push method to be used because we have to create unique id for each message
                mMessageDatabaseReference.push().setValue(modelClass);

                // clearing the input box
                mMessageEditText.setText("");
            }
        });


        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                if(firebaseUser != null){
                    // user is signed in
                //    Toast.makeText(MainActivity.this, "You're now signed in. Welcome to FriendlyChat.", Toast.LENGTH_SHORT).show();
                    onSignedInInitialize(firebaseUser.getDisplayName());
                }
                else{
                    // user is signed out
                    onSignedOutCleanUp();
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setAvailableProviders(Arrays.asList(
                                            new AuthUI.IdpConfig.EmailBuilder().build()))
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        };
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                // Sign-in succeeded, set up the UI
                Toast.makeText(this, "Signed in!", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                // Sign in was canceled by the user, finish the activity
                Toast.makeText(this, "Sign in canceled", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
            else if(resultCode == RC_PHOTO_PICKER && resultCode == RESULT_OK){
                // HERE I CALLED THAT METHOD
                uploadPhotoInFirebase(data);
            }
        }

        private void uploadPhotoInFirebase(@Nullable Intent data){
        Uri selectedImageUri = data.getData();
            // Get a reference to store file at chat_photos/<FILENAME>
            final StorageReference photoRef = storageReference.child(selectedImageUri.getLastPathSegment());

            // Image has been uploaded in the firebase storage
            // Now we need to generate Uri for the image uploaded in the firebase storage
            photoRef.putFile(selectedImageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                            // Download file From Firebase Storage
                            photoRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri downloadPhotoUrl) {
                                    //Now play with downloadPhotoUrl
                                    //Store data into Firebase Realtime Database
                                    ModelClass modelClass = new ModelClass
                                            (mUsername, null, downloadPhotoUrl.toString());
                                    mMessageDatabaseReference.push().setValue(modelClass);
                                }
                            });
                        }
                    });
        }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.sign_out_menu:
                // sign out
                AuthUI.getInstance().signOut(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
        detachDatabaseReadListener();
        mMessageAdapter.clear();
    }

    @Override
    protected void onResume(){
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    private void onSignedInInitialize(String mUsername){
        this.mUsername = mUsername;
        // calling the read database method after the user has logged in the app
        attachDatabaseReadListener();

    }

    private void onSignedOutCleanUp(){
        mUsername = ANONYMOUS;
        mMessageAdapter.clear();
        detachDatabaseReadListener();

    }
    private void attachDatabaseReadListener(){
        if(childEventListener == null) {
            childEventListener = new ChildEventListener() {

                // this method gets called whenever a new message i.e. child is added in the node, even called for existing children whenever
                // a listener is attached
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    ModelClass modelClass = snapshot.getValue(ModelClass.class);
                    mMessageAdapter.add(modelClass);
                }

                // when content of existing chid gets changed
                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                }

                // when a child gets removed
                @Override
                public void onChildRemoved(@NonNull DataSnapshot snapshot) {

                }

                // when one of the child/message changed its position in the node
                @Override
                public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                }

                // when some error occurs
                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };
            // Attaching childEventListener to the database reference
            mMessageDatabaseReference.addChildEventListener(childEventListener);
        }
    }

    private void detachDatabaseReadListener(){
        if(childEventListener != null){
            mMessageDatabaseReference.removeEventListener(childEventListener);
        }
        childEventListener = null;
    }
}