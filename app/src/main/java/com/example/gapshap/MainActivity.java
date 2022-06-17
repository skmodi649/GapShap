package com.example.gapshap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    public static final String ANONYMOUS = "anonymous";

    public static final int MESSAGE_LENGTH_LIMIT = 1000;

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

    // Child event listener for reading data from the realtime database
    private ChildEventListener childEventListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUsername = ANONYMOUS;


        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mMessageDatabaseReference = mFirebaseDatabase.getReference().child("messages");

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


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}