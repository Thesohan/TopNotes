package topnotes.nituk.com.topnotes;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethod;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.DialogFragment;

import static android.content.Context.INPUT_METHOD_SERVICE;

/***
PRO TIP: WHEN NETWORKING ON FRAGMENT AND TRYING TO ACCESS THE SYSTEM RESOURCES , BE CAREFUL WITH THE CONTEXT..
 AS THE HOSTING ACTIVITY MAY HAVE BEEN DETACHED ON COMPLETION OF NETWORK TASK
 ***/
public class UploadDialogFragment extends DialogFragment implements View.OnClickListener {


    private static final int FILE_SELECT_CODE = 0;
    public static final int REQ_CODE = 1;
    public static final int UPLOAD_NOTIFICATION_ID = 2;
    public static final String notificationChannelId = "uplaod_channel";
    private ImageView chooseFileImageViewButton;
    private Button uploadButton;
    private StorageReference mStorageRef;
    private Uri fileUri;

    private ImageView uploadUserImageView;
    private TextView uploadUserNameTextView;
    private int choosenSubject;
    private int choosenType;
    private Activity activity;
    private LinearLayout superLinearLayout;
    private Spinner subjectSpinner, categorySpinner;
    private String notesNameType;
    private String subjectNameType;


    private EditText titleEditText;

    public UploadDialogFragment() {

    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        Dialog dialog = super.onCreateDialog(savedInstanceState);

        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
//       dialog.getWindow().setLocalFocus(true,true);
        return dialog;
        //
    }

    public static UploadDialogFragment getInstance() {
        return new UploadDialogFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.upload_dialog_fragment, container, true);
        // This context should be use throughout the fragment to access any resources related to the hosting activity
        // There was a bug in uploading due to null (getActivity) which occurs due to completion of HTTP request and detachment of
        // the base activity
        activity = getActivity();

        // progressDialog= new ProgressDialog(activity);

        // setup the uploader profile on the upload fragment
        uploadUserImageView = view.findViewById(R.id.uploadUserImageView);
        uploadUserNameTextView = view.findViewById(R.id.uploadUserName);
        uploadUserNameTextView.setText(User.getUser().getName());
        Picasso.get().load(User.getUser().getImageUrl()).into(uploadUserImageView);
        superLinearLayout = view.findViewById(R.id.superUploadFragmentLinearLayout);


        uploadButton = view.findViewById(R.id.uploadButton);
        chooseFileImageViewButton = view.findViewById(R.id.chooseFileImageViewButton);
        mStorageRef = FirebaseStorage.getInstance().getReference();

        superLinearLayout.setOnClickListener(this);
        titleEditText = view.findViewById(R.id.uploadTitleEditText);
        titleEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (i == KeyEvent.KEYCODE_ENTER && keyEvent.getAction() == KeyEvent.ACTION_DOWN)
                    onClick(uploadButton);
                return false;
            }
        });

        Log.i("activity:::", getActivity().toString());


        //To show dropdown list in our app we need to use spinner widget.
        subjectSpinner = view.findViewById(R.id.subjectSpinner);
        ArrayAdapter<String> subjectSpinnerAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, MyApplication.getApp().spinnerSubjectList);
        subjectSpinner.setAdapter(subjectSpinnerAdapter);
        subjectSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                // since the select option is also included
                Log.d("coosenSubject", "" + i);
                choosenSubject = i - 1;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        categorySpinner = view.findViewById(R.id.categorySpinner);
        ArrayAdapter<String> categorysSpinnerAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.spinnerCategoryList));
        categorySpinner.setAdapter(categorysSpinnerAdapter);
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                // since the select option is also included..
                Log.d("catagory", "" + i);

                choosenType = i - 1;

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


        chooseFileImageViewButton.setOnClickListener(this);

        uploadButton.setOnClickListener(this);

        return view;
    }


    private void chooseFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        //need to send mimetypes for some devices (read from stackoverflow not sure

        String[] mimetypes = {"application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // .doc & .docx
                "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation", // .ppt & .pptx
                "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // .xls & .xlsx
                "text/plain",
                "application/pdf",
                "application/zip"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
        try {
            startActivityForResult(intent, FILE_SELECT_CODE);

        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog

            Toast.makeText(getContext(), "Please install a File Manager.",
                    Toast.LENGTH_SHORT).show();
        }
    }

//    private void chooseFile()
//    {

//
//
//        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            intent.setType(mimeTypes.length == 1 ? mimeTypes[0] : "*/*");
//            if (mimeTypes.length > 0) {
//                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
//            }
//        } else {
//            String mimeTypesStr = "";
//            for (String mimeType : mimeTypes) {
//                mimeTypesStr += mimeType + "|";
//            }
//            intent.setType(mimeTypesStr.substring(0,mimeTypesStr.length() - 1));
//        }
//
//
//        intent.addCategory(Intent.CATEGORY_OPENABLE);
//        try {
////            startActivityForResult(
////                    Intent.createChooser(intent, "Select a File to Upload"),
////                    FILE_SELECT_CODE);
//
//            startActivityForResult(intent,FILE_SELECT_CODE);
//
//        } catch (android.content.ActivityNotFoundException ex) {
//            // Potentially direct the user to the Market with a Dialog
//
//            Toast.makeText(getContext(), "Please install a File Manager.",
//                    Toast.LENGTH_SHORT).show();
//        }
//    }

    private void uploadFile(Uri uri) {

        //using current time to set title so their will be no title of similar names:(Removed since content have a fileName now)
//        Calendar calendar=Calendar.getInstance();
//        SimpleDateFormat simpleDateFormat=new SimpleDateFormat("dd-MM-yyyy_HH:mm:ss");
        // final String dateTime=simpleDateFormat.format(calendar.getTime());
        //Uri file = Uri.fromFile(new File("path/to/images/rivers.jpg"));
        StorageReference riversRef = mStorageRef.child("courses")
                .child(MyApplication.getApp().subjectNames.get(choosenSubject))
                .child(activity.getResources().getStringArray(R.array.categoryList)[choosenType])
                .child(getFileName(fileUri));
//
//            progressDialog.setTitle("Uploading...");
//            //Log.i("activity again::",getActivity().toString());
//            // context for the sucess listener
//            progressDialog.show();
        Toast.makeText(activity, "Uploading started....", Toast.LENGTH_SHORT).show();
        riversRef.putFile(uri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Get a URL to the uploaded content

                        taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(activity, new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                //Toast.makeText(activity, "File uploaded successfully, file url:" + uri.toString(), Toast.LENGTH_SHORT).show();
                                Log.i("Upload success, Url:", uri.toString());
                                makeEntryToFBDB(uri.toString());
                                //progressDialog.dismiss();
                                Toast.makeText(activity, "uploaded sucessfully!", Toast.LENGTH_SHORT).show();
                                fileUri = null;
                            }
                        });

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle unsuccessful uploads
                        Toast.makeText(getActivity(), "Uploading Failed," + exception.getMessage(), Toast.LENGTH_SHORT).show();
                        exception.printStackTrace();
//                            progressDialog.dismiss();


                    }
                })
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        double progress = (100 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                        //progressDialog.setMessage((int) progress + "% Uploaded");
                        showDownloadNotification("Uploading..",(int)progress);
                    }
                });

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_SELECT_CODE && resultCode == Activity.RESULT_OK && data != null) {
            fileUri = data.getData();
            Toast.makeText(getContext(), "File ready to upload !", Toast.LENGTH_SHORT).show();
            Log.i("fileuri", fileUri.toString());
            Log.i("path", fileUri.getPath());
            Log.i("info", "fileName:" + getFileName(fileUri) + "and fileSize:" + getFileSize(fileUri));


        }

    }

    // The method saves the upload file's metadata to firebasedatabase
    public void makeEntryToFBDB(String url) {
        UUID contentUUID = UUID.randomUUID();
        Content content = new Content();
        content.setTitle(titleEditText.getText().toString());
        content.setDate(DateFormat.getDateFormat(activity).format(new Date()));
        content.setAuthor(FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
        content.setDownloadUrl(url);
        // new properties
        content.setDownloads(0);
        content.setSubject(MyApplication.getApp().subjectNames.get(choosenSubject));

        Long uriSizeinLong = getFileSize(fileUri);
        Log.i("size in long:", uriSizeinLong.toString());
        String s = uriSizeinLong + "";
        Double uriSize = Double.parseDouble(s);
        Log.i("size in double:", uriSize.toString());
        // cast long to double
        String fileSize = calculateProperFileSize(uriSize);
        content.setSize(fileSize);
        Log.i("uploaded size:", fileSize);


        String fileName = getFileName(fileUri);
        Log.i("fileName:", fileName);
        content.setFileName(fileName);


        // update the myupload list
        sendResult(content);
        FirebaseDatabase.getInstance().getReference("courses").child(MyApplication.getApp().subjectNamesToken.get(choosenSubject))
                .child(activity.getResources().getStringArray(R.array.typeToken)[choosenType])
                .child(contentUUID.toString())
                .setValue(content);
        // createNotification();

    }

    private void sendResult(Content content) {
        Intent intent = new Intent();
        intent.putExtra("content", content);
        Log.i("content with title:", content.getTitle());

        //return the result to that fragment which create this fragment
        getTargetFragment().onActivityResult(getTargetRequestCode(), REQ_CODE, intent);


    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.uploadButton:

                if (fileUri == null) {
                    Toast.makeText(getActivity(), "Please first choose the file", Toast.LENGTH_SHORT).show();
                } else if (MyApplication.getApp().spinnerSubjectList.get(subjectSpinner.getSelectedItemPosition()).equals("Select")) {
                    Toast.makeText(getActivity(), "Please first Choose subject !", Toast.LENGTH_SHORT).show();
                } else if (activity.getResources().getStringArray(R.array.spinnerCategoryList)[categorySpinner.getSelectedItemPosition()].equals("Select")) {
                    Toast.makeText(getActivity(), "Please first Choose category !", Toast.LENGTH_SHORT).show();
                } else if (titleEditText.getText().toString().matches("")) {
                    Toast.makeText(getActivity(), "Please Write the title name !", Toast.LENGTH_SHORT).show();
                } else {
                    checkDuplicacy();
                    DialogFragment dialog = (DialogFragment) getFragmentManager().findFragmentByTag("upload");
                    dialog.dismiss();
                }
                break;

            case R.id.chooseFileImageViewButton:
                //Below code will choose a file from android system

                if (NetworkCheck.isNetworkAvailable(getActivity())) {
                    chooseFile();
                } else {
                    InternetAlertDialogfragment internetAlertDialogfragment = new InternetAlertDialogfragment();
                    internetAlertDialogfragment.show(getFragmentManager().beginTransaction(), "net_dialog");
                }
                break;
            case R.id.superUploadFragmentLinearLayout:
                InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(titleEditText.getWindowToken(), 0);
                break;

        }
    }

//    private void createNotification(){
//
//        NotificationCompat.Builder builder=new NotificationCompat.Builder(activity);
//        builder.setSmallIcon(R.drawable.notes);
//        builder.setContentTitle("New Notes Uploaded");
//        //builder.setContentText(getResources().getStringArray(R.array.subjectList)[choosenSubject] + getResources().getStringArray(R.array.subjectList)[choosenType] +" uploaded ..You can download it..");
//        builder.setContentText(subjectNameType+" "+notesNameType+" uploaded ..You can download it..");
//        Uri defaultRingtone=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//        builder.setSound(defaultRingtone);
//        Intent intent=new Intent(activity,SubjectListActivity.class);
//        PendingIntent pendingIntent=PendingIntent.getActivity(activity,0,intent,PendingIntent.FLAG_UPDATE_CURRENT);
//        builder.setContentIntent(pendingIntent);
//        NotificationManager notificationManager=(NotificationManager)activity.getSystemService(Context.NOTIFICATION_SERVICE);
//        notificationManager.notify(0,builder.build());
//    }

    public String calculateProperFileSize(double bytes) {
        String[] fileSizeUnits = {"bytes", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};
        String sizeToReturn = "";// = FileUtils.byteCountToDisplaySize(bytes), unit = "";
        int index = 0;
        for (index = 0; index < fileSizeUnits.length; index++) {
            if (bytes < 1024) {
                break;
            }
            bytes = bytes / 1024;
        }
        System.out.println("Systematic file size: " + bytes + " " + fileSizeUnits[index]);
        sizeToReturn = String.valueOf(new DecimalFormat("##.##").format(bytes)) + " " + fileSizeUnits[index];
        return sizeToReturn;
    }

//    private Long getFileSize(Uri uri)
//    {   long dataSize =0;
//        File f=null;
//        String scheme = uri.getScheme();
//        System.out.println("Scheme type " + scheme);
//        if(scheme.equals(ContentResolver.SCHEME_CONTENT))
//        {
//            try {
//                InputStream fileInputStream=activity.getApplicationContext().getContentResolver().openInputStream(uri);
//                dataSize = fileInputStream.available();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            System.out.println("File size in bytes"+dataSize);
//
//        }
//        else if(scheme.equals(ContentResolver.SCHEME_FILE))
//        {
//            String path = uri.getPath();
//            try {
//                f = new File(path);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            System.out.println("File size in bytes"+f.length());
//        }
//
//        return dataSize;
//    }
//
//    public String getFileName(Uri uri)
//    {
//       String path = uri.getPath();
//       String splits[] = path.split(":");
//       Log.i("splits","first:"+splits[0]+"and second:"+splits[1]);
//
//       return splits[1];
//    }

    private Long getFileSize(Uri uri) {
        int index = 0;
        Cursor returnCursor =
                activity.getContentResolver().query(uri, null, null, null, null);
        returnCursor.moveToFirst();
        try {

            index = returnCursor.getColumnIndex(OpenableColumns.SIZE);
            return returnCursor.getLong(index);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            returnCursor.close();
        }
        return null;
    }

    public String getFileName(Uri uri) {

        int index = 0;
        Cursor returnCursor = activity.getContentResolver().query(uri, null, null, null, null);
        returnCursor.moveToFirst();
        try {
            index = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            return returnCursor.getString(index);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            returnCursor.close();
        }

        return "";
    }

    public void checkDuplicacy() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("courses").child(MyApplication.getApp().subjectNamesToken.get(choosenSubject))
                .child(activity.getResources().getStringArray(R.array.typeToken)[choosenType]);

        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> fileNames = new ArrayList<>();

                for (DataSnapshot dsp : dataSnapshot.getChildren()) {
                    fileNames.add(dsp.getValue(Content.class).getFileName());
                }

                Log.i("fileOnserver", fileNames.toString());

                if (!fileNames.contains(getFileName(fileUri))) // no duplicate file
                {
                    uploadFile(fileUri);
                } else {
                    Toast.makeText(activity, "The file already exists on our servers", Toast.LENGTH_LONG).show();
                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


        public void showDownloadNotification (String title,int progress){

            createNotificationChannel(); //notification won't work without this in android version above 8.0+

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(activity, notificationChannelId);
            //this channelid should be unique and it is used to track the notification

//            Intent intent = new Intent();
//            PendingIntent pendingIntent = PendingIntent.getActivity(activity, 0, intent, 0);

                     if(progress<100) {
                         notificationBuilder.setSmallIcon(R.drawable.my_uploads)
                                 .setContentTitle("Uploading...")
                                 .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                                 .setPriority(NotificationCompat.DEFAULT_ALL) //to support Android 7.1 and lower.
                                 .setContentText(progress + "%" + " uploaded")
                                 .setProgress(100, progress, false)
                                 .setOngoing(true);
                     }else{
                         notificationBuilder.setSmallIcon(R.mipmap.ic_launcher_round)
                                 .setContentTitle("Success")
                                 .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                                 .setPriority(NotificationCompat.DEFAULT_ALL) //to support Android 7.1 and lower.
                                 .setContentText(progress+"% uploaded")
                                 .setSubText("File uploaded sucessfully")
                                 .setProgress(100, progress, false);
                     }


            //to display notification
            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(activity);

            notificationManagerCompat.notify(UPLOAD_NOTIFICATION_ID, notificationBuilder.build());
            // to track the current notification
        }

        private void createNotificationChannel(){
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                int importance = NotificationManager.IMPORTANCE_DEFAULT;
                NotificationChannel channel = new NotificationChannel(notificationChannelId, "TopNotes", importance);
                channel.setDescription("none");
                // Register the channel with the system; you can't change the importance
                // or other notification behaviors after this
                NotificationManager notificationManager = activity.getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
            }
        }



    }


