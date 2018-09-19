package com.example.suvamjain.musicclub;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.opengl.Visibility;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.suvamjain.musicclub.modals.Booking;
import com.example.suvamjain.musicclub.modals.Users;
import com.example.suvamjain.musicclub.other.ConnectivityReceiver;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity implements ConnectivityReceiver.ConnectivityReceiverListener, NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "Main Activity" ;
    private FirebaseAuth.AuthStateListener authListener;
    private FirebaseAuth auth;
    private static final int PICKFILE_REQUEST_CODE = 1234;
    private CircleImageView nav_profile;
    private TextView userName, userEmail;
    private byte[] mUploadBytes;
    private StorageReference mProfileImageStorageReference;
    private DatabaseReference mUserDbRef;
    private Uri downloadUrl,uri;
    private SharedPreferences.Editor loginPrefsEditor;
    private ConnectivityReceiver connectivityReceiver;

    private TextView mTextMessage;
    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private FrameLayout mFrameLayout;
    private RelativeLayout mParent;
    private int[] tabIcons = {
            R.drawable.ic_favourite,
            R.drawable.ic_music_booking,
            R.drawable.ic_music
    };
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    //mTextMessage.setText(R.string.title_home);
                    return true;
                case R.id.navigation_dashboard:
//                    LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//                    View g = vi.inflate(R.layout.music_loader,null);
//                    ImageView v = g.findViewById(R.id.loader);
//                    Glide.with(MainActivity.this).load(R.drawable.music_loader).into(v);
//                    PopupWindow popupWindow = new PopupWindow(g, 300, 300, false);
//                    popupWindow.showAtLocation(g, Gravity.CENTER, 0, 0);
                    //mTextMessage.setText(R.string.title_dashboard);
                    LoadMusic(View.VISIBLE);
                    return true;
                case R.id.navigation_notifications:
                    //mTextMessage.setText(R.string.title_notifications);
                    return true;
            }
            return false;
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //checkConnection();

        // check user is logged in or not? get firebase auth instance
        auth = FirebaseAuth.getInstance();
        authListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                Log.e(TAG, "firebase user: " + user);
                if (user == null) {
                    Log.e("Main activity","Auth state changed, opening Login");
                    // user auth state is changed - user is null
                    // launch login activity
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    finish();
                }
            }
        };

        connectivityReceiver = new ConnectivityReceiver();
        setContentView(R.layout.activity_main);
        mParent = (RelativeLayout)findViewById(R.id.parent_container);
        mFrameLayout = (FrameLayout)findViewById(R.id.loader_container);
        viewPager = (ViewPager) findViewById(R.id.viewpager);
        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        SharedPreferences loginPreferences = getSharedPreferences("loginPrefs", MODE_PRIVATE);
        loginPrefsEditor = loginPreferences.edit();

        //mUserDbRef = FirebaseDatabase.getInstance().getReference().child("users").child(auth.getUid()).getRef();
//        mProfileImageStorageReference = FirebaseStorage.getInstance().getReference().child("profile_photos");

        setSupportActionBar(toolbar);

        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        View headerView = navigationView.getHeaderView(0);
        nav_profile = (de.hdodenhof.circleimageview.CircleImageView) headerView.findViewById(R.id.nav_profile_image);
        userName = headerView.findViewById(R.id.nav_userName);
        userEmail = headerView.findViewById(R.id.nav_userEmail);

        if (loginPreferences.contains("User")) {
            String json = loginPreferences.getString("User", "");
            Users obj = new Gson().fromJson(json, Users.class);
            userName.setText(obj.getName());
            userEmail.setText(obj.getEmail());
            mUserDbRef = FirebaseDatabase.getInstance().getReference().child("users").child(obj.getKey()).getRef();
        }
        mProfileImageStorageReference = FirebaseStorage.getInstance().getReference().child("profile_photos");

        nav_profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(MainActivity.this, "clicked", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, PICKFILE_REQUEST_CODE);
            }
        });

        //this will control the hamburger button of drawer to open nav drawer from right
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawer.isDrawerOpen(GravityCompat.END)) {
                    drawer.closeDrawer(GravityCompat.END);
                } else {
                    drawer.openDrawer(GravityCompat.END);
                }
            }
        });

        mUserDbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.e(TAG,"user db snapshot " + dataSnapshot.getValue().toString());
                Log.e(TAG,"updating login prefs user as profileImg is updated");
                Users updateuser = dataSnapshot.getValue(Users.class);
                updateuser.setKey(dataSnapshot.getKey());
                String json = new Gson().toJson(updateuser);
                loginPrefsEditor.putString("User", json);
                loginPrefsEditor.apply();
                userName.setText(updateuser.getName());
                userEmail.setText(updateuser.getEmail());
                if (dataSnapshot.hasChild("profileImg"))
                    Picasso.with(MainActivity.this).load(dataSnapshot.child("profileImg").getValue().toString())
                            .error(R.drawable.user_avatar).placeholder(R.drawable.user_avatar).fit().centerInside().noFade().into(nav_profile);
                else {
                    String gender = dataSnapshot.child("gender").getValue().toString();
                    switch (gender) {
                        case "Male":
                            nav_profile.setImageDrawable(getResources().getDrawable(R.drawable.male_avatar));
                            break;
                        case "Female":
                            nav_profile.setImageDrawable(getResources().getDrawable(R.drawable.woman_avatar));
                            break;
                        default:
                            nav_profile.setImageDrawable(getResources().getDrawable(R.drawable.user_avatar));
                            break;
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                nav_profile.setImageDrawable(getResources().getDrawable(R.drawable.user_avatar)); }
        });

        setupViewPager(viewPager);
        setupTabIcons();

        //mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }

    private void setupTabIcons() {
        tabLayout.getTabAt(0).setIcon(tabIcons[0]);
        tabLayout.getTabAt(1).setIcon(tabIcons[1]);
        tabLayout.getTabAt(2).setIcon(tabIcons[2]);
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new HomeFragment(), "EVENTS");
        adapter.addFragment(new BookingFragment(), "BOOKINGS");
        adapter.addFragment(new HomeFragment(), "CHATS");
        //adapter.addFragment(new HomeFragment(), "FOUR");
        viewPager.setAdapter(adapter);
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) { return mFragmentList.get(position); }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
            //return null;  //return null instead of upper line to only display icon for tabs without title.
        }
    }

    public void LoadMusic(int visibility){

        MusicLoaderFragment fragment = new MusicLoaderFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        transaction.replace(R.id.loader_container, fragment, "loader");
        transaction.addToBackStack("loader");
        transaction.commit();
        mFrameLayout.setVisibility(visibility);
//        Handler handler = new Handler();
//        handler.postDelayed(new Runnable() {
//            public void run() {
//                mFrameLayout.setVisibility(visibility);
//            }
//        }, 3500); // 3 sec delay
    }

    public void showSnackMsg(String msg) {
        Snackbar snackbar = Snackbar
                .make(findViewById(R.id.parent_container), msg, Snackbar.LENGTH_LONG)
                .setAction("UNDO", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Snackbar snackbar1 = Snackbar.make(mParent, "Message is restored!", Snackbar.LENGTH_SHORT);
                        snackbar1.show();
                    }
                });

        // Changing Action button text color
        snackbar.setActionTextColor(Color.RED);

        // Changing snack text msg color
        View sbView = snackbar.getView();
        TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
        //textView.setTextColor(Color.YELLOW);
        snackbar.show();
    }

    // Showing the status in Snackbar
    private void showSnackMsg(boolean isConnected) {
        String message;
        int color;
        if (isConnected) {
//            message = "Good! Connected to Internet";
//            color = Color.WHITE;
        } else {
            message = "Sorry! Not connected to internet";
            color = Color.RED;
            Snackbar snackbar = Snackbar.make(findViewById(R.id.parent_container), message, Snackbar.LENGTH_LONG);
            View sbView = snackbar.getView();
            TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
            textView.setTextColor(color);
            snackbar.show();
        }

//        Snackbar snackbar = Snackbar.make(findViewById(R.id.parent_container), message, Snackbar.LENGTH_LONG);
//        View sbView = snackbar.getView();
//        TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
//        textView.setTextColor(color);
//        snackbar.show();
    }

    // Method to manually check connection status
    private void checkConnection() {
        Log.e("Checking conn manual","------->");
        boolean isConnected = ConnectivityReceiver.isConnected();
        showSnackMsg(isConnected);
    }

    private void uploadNewPhoto(Uri imagePath){
        Log.d(TAG, "uploadNewPhoto: uploading a new nav image uri to storage");
        BackgroundImageResize resize = new BackgroundImageResize(null);
        resize.execute(imagePath);
    }

    public class BackgroundImageResize extends AsyncTask<Uri, Integer, byte[]> {
        Bitmap mBitmap;
        BackgroundImageResize(Bitmap bitmap) {
            if(bitmap != null){
                this.mBitmap = bitmap;
            }
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.e(TAG, "Compressing Image");
        }
        @Override
        protected byte[] doInBackground(Uri... params) {
            Log.e(TAG, "doInBackground: started.");
            if(mBitmap == null){
                try{
                    mBitmap = MediaStore.Images.Media.getBitmap(MainActivity.this.getContentResolver(), params[0]);
                } catch (IOException e){
                    Log.e(TAG, "doInBackground: IOException: " + e.getMessage());
                }
            }
            byte[] bytes = null;
            bytes = getBytesFromBitmap(mBitmap, 75); //compressing the image by keeping 75% quality
            return bytes;
        }
        @Override
        protected void onPostExecute(byte[] bytes) {
            super.onPostExecute(bytes);
            mUploadBytes = bytes;
            //execute the upload task
            executeUploadTask();
        }
    }

    public static byte[] getBytesFromBitmap(Bitmap bitmap, int quality){
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality,stream);
        return stream.toByteArray();
    }

    private void executeUploadTask() {
        final StorageReference photoRef = mProfileImageStorageReference.child(auth.getUid() + "_image");
        //UploadTask uploadTask = photoRef.putBytes(mUploadBytes);
        photoRef.putBytes(mUploadBytes).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                photoRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        //insert the download url into the firebase database
                        downloadUrl = uri;
                        Log.e(TAG, "onSuccess: firebase download url: " + downloadUrl.toString());
                        mUserDbRef.child("profileImg").setValue(downloadUrl.toString());
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                showSnackMsg("Profile Image upload failed!!");
                Log.e(TAG, "onFailure: " + e.toString());
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case R.id.refresh_bookings:
////                auth.signOut();
//                 showSnackMsg("Refresh clicked");
//                 BookingFragment bf = new BookingFragment();
//                 bf.loadAllBookings();
//                return true;
//            default:
//                return super.onOptionsItemSelected(item);
//        }
//    }

    /**
     * Callback will be triggered when there is change in
     * network connection
     */
    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {
        Log.e("Ntwrk Changed"," from Conn changed method ------->");
        showSnackMsg(isConnected);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e("Checking conn manual"," from RESUME method ------->");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            final IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

            //ConnectivityReceiver connectivityReceiver = new ConnectivityReceiver();
            registerReceiver(connectivityReceiver, intentFilter);
        }

        // register connection status listener
        MyApplication.getInstance().setConnectivityListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(connectivityReceiver);
    }

    @Override
    public void onStart() {
        super.onStart();
        auth.addAuthStateListener(authListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (authListener != null) {
            auth.removeAuthStateListener(authListener);
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        switch (id) {
            case R.id.nav_camera:
                // Handle the camera action
                break;
            case R.id.nav_gallery:

                break;
            case R.id.nav_slideshow:

                break;
            case R.id.nav_manage:

                break;
            case R.id.nav_share:

                break;
            case R.id.nav_send:

                break;
            case R.id.nav_logOut:
                auth.signOut();
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.END);
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//            Results when selecting a new image from memory
        if(requestCode == PICKFILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri selectedImageUri = data.getData();
            Log.d("nav image", "onActivityResult: image uri: " + selectedImageUri);
            uploadNewPhoto(selectedImageUri);
            Picasso.with(getBaseContext()).load(selectedImageUri.toString()).fit().centerInside().noFade().into(nav_profile);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.END)) {
            drawer.closeDrawer(GravityCompat.END);
        } else {
            super.onBackPressed();
        }
    }
}
