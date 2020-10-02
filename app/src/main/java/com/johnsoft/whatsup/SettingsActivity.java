package com.johnsoft.whatsup;
import androidx.annotation.NonNull;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {
    private Button UpdateAccountButton;
    private EditText userName, userStatus;
    private CircleImageView userProfileImage;
    public static final int GALLERY_REQUEST = 1;
    private String currentUserID, downloadUrl;
    private FirebaseAuth mAuth;
    private DatabaseReference RootRef;
    private StorageReference userProfileImageRef;
    private ProgressDialog loadingBar;
    private Uri imageUri;
    private String setUserName, setUserStatus;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        RootRef = FirebaseDatabase.getInstance().getReference();
        userProfileImageRef = FirebaseStorage.getInstance().getReference().child("Profile Images");
        initializeFields();

        userName.setVisibility(View.INVISIBLE);

        UpdateAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UpdateSettings();
            }
        });

        RetrieveUserInfo();

        userProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent imageIntent = new Intent(Intent.ACTION_GET_CONTENT);
                imageIntent.setType("image/*");
                startActivityForResult(imageIntent, GALLERY_REQUEST);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GALLERY_REQUEST && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            userProfileImage.setImageURI(imageUri);
        }
    }

    private void initializeFields() {
        UpdateAccountButton = findViewById(R.id.update_settings_button);
        userName = findViewById(R.id.set_user_name);
        userStatus = findViewById(R.id.set_profile_status);
        userProfileImage = findViewById(R.id.set_profile_image);
        loadingBar = new ProgressDialog(this);
    }
    private void UpdateSettings() {
        setUserName = userName.getText().toString().trim();
        setUserStatus = userStatus.getText().toString().trim();


        if (userProfileImage == null){
            //Picasso.get().load(imageUri).placeholder(R.drawable.profile).into(userProfileImage);
            Toast.makeText(this, "Please set profile image", Toast.LENGTH_SHORT).show();
        }
        if (TextUtils.isEmpty(setUserName)){
            Toast.makeText(this, "Please set the username...", Toast.LENGTH_SHORT).show();
        }
        if (TextUtils.isEmpty(setUserStatus)){
            Toast.makeText(this, "Please set your status...", Toast.LENGTH_SHORT).show();
        }else {
//            HashMap<String, String> profileMap = new HashMap<>();
//            profileMap.put("uid", currentUserID);
//            profileMap.put("name", setUserName);
//            profileMap.put("status", setUserStatus);
//            RootRef.child("Users").child(currentUserID).setValue(profileMap).addOnCompleteListener(new OnCompleteListener<Void>() {
//                @Override
//                public void onComplete(@NonNull Task<Void> task) {
//                    if (task.isSuccessful()){
//                        sendUserToMainActivity();
//                        Toast.makeText(SettingsActivity.this, "Profile updated successfully...", Toast.LENGTH_SHORT).show();
//                    }else {
//                        String message = task.getException().toString();
//                        Toast.makeText(SettingsActivity.this, "Error : " + message, Toast.LENGTH_SHORT).show();
//                    }
//        }

//            });

            saveUsersInfo();

        }
    }

    private void saveUsersInfo() {
        loadingBar.setTitle("Setting up profile");
        loadingBar.setMessage("Please wait, while we are setting your profile");
        loadingBar.setCanceledOnTouchOutside(false);
        loadingBar.show();
        final StorageReference filePath = userProfileImageRef.child(currentUserID + ".jpg");
        final UploadTask uploadTask = filePath.putFile(imageUri);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                String message = e.toString();
                Toast.makeText(SettingsActivity.this, "Error : " + message, Toast.LENGTH_SHORT).show();
                loadingBar.dismiss();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(SettingsActivity.this, "Image uploaded successfully...", Toast.LENGTH_SHORT).show();

                Task<Uri> uriTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                        if (!task.isSuccessful()){
                            throw task.getException();
                        }
                        downloadUrl = filePath.getDownloadUrl().toString();
                        return filePath.getDownloadUrl();

                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (task.isSuccessful()){
                            downloadUrl = task.getResult().toString();
                            Toast.makeText(SettingsActivity.this, "Profile image saved to database successfully... ", Toast.LENGTH_SHORT).show();
                            saveUsersInfoToDatabase();
                        }
                    }
                });

            }
        });
    }

    private void saveUsersInfoToDatabase() {
        HashMap<String, String> profileMap = new HashMap<>();
            profileMap.put("uid", currentUserID);
            profileMap.put("name", setUserName);
            profileMap.put("status", setUserStatus);
            profileMap.put("image", downloadUrl);
            RootRef.child("Users").child(currentUserID).setValue(profileMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()){
                        sendUserToMainActivity();
                        loadingBar.dismiss();
                        Toast.makeText(SettingsActivity.this, "Profile updated successfully...", Toast.LENGTH_SHORT).show();
                    }else {
                        loadingBar.dismiss();
                        String message = task.getException().toString();
                        Toast.makeText(SettingsActivity.this, "Error : " + message, Toast.LENGTH_SHORT).show();
                    }
        }

            });
    }

    private void RetrieveUserInfo(){
        RootRef.child("Users").child(currentUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if ((snapshot.exists()) && (snapshot.hasChild("name") && (snapshot.hasChild("image")))){
                    String retrieveUserName = snapshot.child("name").getValue().toString();
                    String retrieveUserStatus = snapshot.child("status").getValue().toString();
                    String retrieveProfileImage = snapshot.child("image").getValue().toString();

                    userName.setText(retrieveUserName);
                    userStatus.setText(retrieveUserStatus);
                    Picasso.get().load(retrieveProfileImage).into(userProfileImage);

                }else if ((snapshot.exists()) && (snapshot.hasChild("name"))){
                    String retrieveUserName = snapshot.child("name").getValue().toString();
                    String retrieveUserStatus = snapshot.child("status").getValue().toString();

                    userName.setText(retrieveUserName);
                    userStatus.setText(retrieveUserStatus);

                }else {
                    userName.setVisibility(View.VISIBLE);
                    Toast.makeText(SettingsActivity.this, "Please set & and update your profile information", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void sendUserToMainActivity(){
        Intent mainIntent = new Intent(SettingsActivity.this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }
}