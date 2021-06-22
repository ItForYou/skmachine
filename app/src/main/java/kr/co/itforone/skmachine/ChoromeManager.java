package kr.co.itforone.skmachine;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.JsResult;
import android.webkit.MimeTypeMap;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Toast;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

// 웹 브라우저 이벤트 구현
class ChoromeManager extends WebChromeClient {
    MainActivity mainActivity;
    Activity activity;

    ChoromeManager(MainActivity mainActivity, Activity activity) {
        this.mainActivity = mainActivity;
        this.activity = activity;
    }

    ChoromeManager(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    ChoromeManager() {
    }

    //어럴트 창 처리
    @Override
    public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
        new AlertDialog.Builder(view.getContext())
                .setTitle("")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                result.confirm();
                            }
                        })
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                result.cancel();
                            }
                        })
                .setCancelable(false)
                .create()
                .show();
        return true;
    }

    public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
        new AlertDialog.Builder(view.getContext())
                .setTitle("")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok,
                        new AlertDialog.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                result.confirm();
                            }
                        })
                .setCancelable(false)
                .create()
                .show();

        return true;
    }

    // 웹뷰 업로드 START
    // For Android < 3.0
    public void openFileChooser(ValueCallback<Uri> uploadMsg) {
        openFileChooser(uploadMsg, "");
    }

    // For Android 3.0+
    public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
        mainActivity.filePathCallbackNormal = uploadMsg;
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        i.setType("video/*");

        mainActivity.startActivityForResult(Intent.createChooser(i, "File Chooser"), mainActivity.FILECHOOSER_NORMAL_REQ_CODE);
    }

    // For Android 4.1+
    public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
        openFileChooser(uploadMsg, acceptType);
    }

    // For Android 5.0+
    public boolean onShowFileChooser(
            WebView webView, ValueCallback<Uri[]> filePathCallback,
            FileChooserParams fileChooserParams) {

        mainActivity.filePathCallbackLollipop = filePathCallback;

        // 파일 선택
        // Create AndroidExampleFolder at sdcard
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            int cameraPerChk = ContextCompat.checkSelfPermission(mainActivity, Manifest.permission.CAMERA);
            if (cameraPerChk == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(mainActivity, "앱 정보에서 카메라 권한을 허용해주세요.", Toast.LENGTH_SHORT).show();
            }

            File imageStorageDir = mainActivity.getFilesDir();
            if (!imageStorageDir.exists()) {
                // Create AndroidExampleFolder at sdcard
                imageStorageDir.mkdirs();
            }
            File file = new File(imageStorageDir, "IMG_" + String.valueOf(System.currentTimeMillis()) + ".jpg");
            mainActivity.mCapturedImageURI = FileProvider.getUriForFile(mainActivity, mainActivity.getApplicationContext().getPackageName() + ".fileprovider", file);

        } else {
            File imageStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Jumpingcat");
            if (!imageStorageDir.exists()) {
                // Create AndroidExampleFolder at sdcard
                imageStorageDir.mkdirs();
            }
            // Create camera captured image file path and name
            File file = new File(imageStorageDir + File.separator + "IMG_" + String.valueOf(System.currentTimeMillis()) + ".jpg");
            mainActivity.mCapturedImageURI = Uri.fromFile(file);
        }

        Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mainActivity.mCapturedImageURI);

        String acceptTypes[] = fileChooserParams.getAcceptTypes();
        String acceptType = "";
        for (int i = 0; i < acceptTypes.length; ++i) {
            if (acceptTypes[i] != null && acceptTypes[i].length() != 0)
                acceptType += acceptTypes[i] + ";";
        }
//        Log.d("로그:acceptTypes Arr", Arrays.toString(acceptTypes));
//        Log.d("로그:acceptType", acceptTypes[0] + "");

        Intent i = new Intent(Intent.ACTION_PICK);

        if (acceptTypes[0].equals("video/*")) {
            i.setType(MediaStore.Video.Media.CONTENT_TYPE);
            i.setData(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        } else {
            i.setType(MediaStore.Images.Media.CONTENT_TYPE);
            i.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        }


        // Create file chooser intent
        Intent chooserIntent = Intent.createChooser(i, "사진을 가져올 방법을 선택하세요.");
        // Set camera intent to file chooser
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Parcelable[]{captureIntent});

        // On select image call onActivityResult method of activity
        mainActivity.startActivityForResult(chooserIntent, mainActivity.FILECHOOSER_LOLLIPOP_REQ_CODE);

        /*
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType(MediaStore.Images.Media.CONTENT_TYPE);
        i.setType("video/*");

        mainActivity.startActivityForResult(Intent.createChooser(i, "파일을 선택하세요"), mainActivity.FILECHOOSER_LOLLIPOP_REQ_CODE);
         */

        return true;
    }
    // 웹뷰 업로드 END

    /*
    // Validation utility for mime types
    private List<String> extractValidMimeTypes(String[] mimeTypes) {
        List<String> results = new ArrayList<String>();
        List<String> mimes;
        if (mimeTypes.length == 1 && mimeTypes[0].contains(",")) {
            mimes = Arrays.asList(mimeTypes[0].split(","));
        } else {
            mimes = Arrays.asList(mimeTypes);
        }
        MimeTypeMap mtm = MimeTypeMap.getSingleton();
        for (String mime : mimes) {
            if (mime != null && mime.trim().startsWith(".")) {
                String extensionWithoutDot = mime.trim().substring(1, mime.trim().length());
                String derivedMime = mtm.getMimeTypeFromExtension(extensionWithoutDot);
                if (derivedMime != null && !results.contains(derivedMime)) {
                    // adds valid mime type derived from the file extension
                    results.add(derivedMime);
                }
            } else if (mtm.getExtensionFromMimeType(mime) != null && !results.contains(mime)) {
                // adds valid mime type checked agains file extensions mappings
                results.add(mime);
            }
        }
        return results;
    }

     */


}
