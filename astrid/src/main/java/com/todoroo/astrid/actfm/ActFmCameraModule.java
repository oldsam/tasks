/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.actfm;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.widget.ArrayAdapter;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;

import org.tasks.R;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class ActFmCameraModule {

    protected static final int REQUEST_CODE_CAMERA = 1;
    protected static final int REQUEST_CODE_PICTURE = 2;

    private static File lastTempFile = null;

    public interface ClearImageCallback {
        public void clearImage();
    }

    public static void showPictureLauncher(final Fragment fragment, final ClearImageCallback clearImageOption) {
        ArrayList<String> options = new ArrayList<>();

        final Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        PackageManager pm = fragment.getActivity().getPackageManager();
        final boolean cameraAvailable = pm.queryIntentActivities(cameraIntent, 0).size() > 0;
        if(cameraAvailable) {
            options.add(fragment.getString(R.string.actfm_picture_camera));
        }

        options.add(fragment.getString(R.string.actfm_picture_gallery));

        if (clearImageOption != null) {
            options.add(fragment.getString(R.string.actfm_picture_clear));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(fragment.getActivity(),
                android.R.layout.simple_spinner_dropdown_item, options.toArray(new String[options.size()]));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int which) {
                if(which == 0 && cameraAvailable) {
                    lastTempFile = getTempFile(fragment.getActivity());
                    if (lastTempFile != null) {
                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(lastTempFile));
                    }
                    fragment.startActivityForResult(cameraIntent, REQUEST_CODE_CAMERA);
                } else if ((which == 1 && cameraAvailable) || (which == 0 && !cameraAvailable)) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    fragment.startActivityForResult(Intent.createChooser(intent,
                            fragment.getString(R.string.actfm_TVA_tag_picture)), REQUEST_CODE_PICTURE);
                } else {
                    if (clearImageOption != null) {
                        clearImageOption.clearImage();
                    }
                }
            }
        };

        // show a menu of available options
        new AlertDialog.Builder(fragment.getActivity())
        .setAdapter(adapter, listener)
        .show().setOwnerActivity(fragment.getActivity());
    }

    private static File getTempFile(Activity activity) {
        try {
            String storageState = Environment.getExternalStorageState();
            if(storageState.equals(Environment.MEDIA_MOUNTED)) {
                String path = Environment.getExternalStorageDirectory().getName() + File.separatorChar + "Android/data/" + activity.getPackageName() + "/files/";
                return File.createTempFile("comment_pic_" + DateUtilities.now(), ".jpg", new File(path));
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    public interface CameraResultCallback {
        public void handleCameraResult(Bitmap bitmap);
    }

    private static Bitmap bitmapFromUri(Activity activity, Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = activity.managedQuery(uri, projection, null, null, null);
        String path;

        if(cursor != null) {
            int column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            path = cursor.getString(column_index);
        } else {
            path = uri.getPath();
        }

        return AndroidUtilities.readScaledBitmap(path);
    }

    public static boolean activityResult(Activity activity, int requestCode, int resultCode, Intent data,
            CameraResultCallback cameraResult) {
        if(requestCode == ActFmCameraModule.REQUEST_CODE_CAMERA && resultCode == Activity.RESULT_OK) {
            Bitmap bitmap;
            if (data == null) { // large from camera
                if (lastTempFile != null) {
                    bitmap = bitmapFromUri(activity, Uri.fromFile(lastTempFile));
                    lastTempFile.deleteOnExit();
                    lastTempFile = null;
                }
                else {
                    bitmap = null;
                }
            } else {
                bitmap = data.getParcelableExtra("data"); //$NON-NLS-1$
            }
            if(bitmap != null) {
                activity.setResult(Activity.RESULT_OK);
                cameraResult.handleCameraResult(bitmap);
            }
            return true;
        } else if(requestCode == ActFmCameraModule.REQUEST_CODE_PICTURE && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            Bitmap bitmap = bitmapFromUri(activity, uri);
            if(bitmap != null) {
                activity.setResult(Activity.RESULT_OK);
                cameraResult.handleCameraResult(bitmap);
            }
            return true;
        }
        return false;
    }

}
