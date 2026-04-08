package com.mad.cameragallery;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements ImageAdapter.OnImageClickListener {
    private static final String PREFS = "camera_gallery_prefs";
    private static final String KEY_TREE_URI = "tree_uri";

    private final List<ImageItem> imageItems = new ArrayList<>();

    private RecyclerView recyclerView;
    private TextView folderNameLabel;
    private TextView folderPathLabel;
    private TextView emptyStateLabel;
    private ImageAdapter imageAdapter;

    private Uri selectedFolderUri;
    private Uri pendingPhotoUri;

    private final ActivityResultLauncher<Uri> folderPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), this::handleFolderSelected);

    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success) {
                    Toast.makeText(this, R.string.photo_saved, Toast.LENGTH_SHORT).show();
                    loadImages();
                } else {
                    deletePendingPhoto();
                }
                pendingPhotoUri = null;
            });

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    launchCameraCapture();
                } else {
                    Toast.makeText(this, R.string.camera_permission_denied, Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> detailLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    loadImages();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        setupRecyclerView();
        setupClicks();
        restoreSelectedFolder();
    }

    private void bindViews() {
        recyclerView = findViewById(R.id.recyclerView);
        folderNameLabel = findViewById(R.id.folderNameLabel);
        folderPathLabel = findViewById(R.id.folderPathLabel);
        emptyStateLabel = findViewById(R.id.emptyStateLabel);
    }

    private void setupRecyclerView() {
        imageAdapter = new ImageAdapter(this);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerView.setAdapter(imageAdapter);
    }

    private void setupClicks() {
        MaterialButton chooseFolderButton = findViewById(R.id.chooseFolderButton);
        MaterialButton capturePhotoButton = findViewById(R.id.capturePhotoButton);
        MaterialButton refreshButton = findViewById(R.id.refreshButton);

        chooseFolderButton.setOnClickListener(view -> folderPickerLauncher.launch(selectedFolderUri));
        capturePhotoButton.setOnClickListener(view -> ensureCameraPermissionAndCapture());
        refreshButton.setOnClickListener(view -> loadImages());
    }

    private void restoreSelectedFolder() {
        SharedPreferences preferences = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String treeUriValue = preferences.getString(KEY_TREE_URI, null);
        if (treeUriValue == null) {
            showEmptyState(true);
            return;
        }

        selectedFolderUri = Uri.parse(treeUriValue);
        updateFolderLabels();
        loadImages();
    }

    private void handleFolderSelected(@Nullable Uri uri) {
        if (uri == null) {
            return;
        }

        try {
            getContentResolver().takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            );
        } catch (SecurityException ignored) {
            // Some providers persist automatically.
        }

        selectedFolderUri = uri;
        getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_TREE_URI, uri.toString())
                .apply();

        updateFolderLabels();
        loadImages();
    }

    private void updateFolderLabels() {
        if (selectedFolderUri == null) {
            folderNameLabel.setText(R.string.no_folder_selected);
            folderPathLabel.setText(R.string.choose_folder_prompt);
            return;
        }

        DocumentFile folder = DocumentFile.fromTreeUri(this, selectedFolderUri);
        String folderName = folder != null && folder.getName() != null
                ? folder.getName()
                : getString(R.string.selected_folder);
        String readableFolderPath = ImageMetadataUtils.resolveReadablePath(selectedFolderUri);

        folderNameLabel.setText(getString(R.string.folder_name, folderName));
        folderPathLabel.setText(getString(R.string.folder_location, readableFolderPath));
    }

    private void ensureCameraPermissionAndCapture() {
        if (selectedFolderUri == null) {
            Toast.makeText(this, R.string.select_folder_first, Toast.LENGTH_SHORT).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCameraCapture();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCameraCapture() {
        DocumentFile folder = DocumentFile.fromTreeUri(this, selectedFolderUri);
        if (folder == null || !folder.canWrite()) {
            Toast.makeText(this, R.string.unable_to_write_folder, Toast.LENGTH_SHORT).show();
            return;
        }

        String filename = "IMG_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date()) + ".jpg";
        DocumentFile createdFile = folder.createFile("image/jpeg", filename);
        if (createdFile == null) {
            Toast.makeText(this, R.string.file_create_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        pendingPhotoUri = createdFile.getUri();
        takePictureLauncher.launch(pendingPhotoUri);
    }

    private void deletePendingPhoto() {
        if (pendingPhotoUri == null) {
            return;
        }

        try {
            DocumentsContract.deleteDocument(getContentResolver(), pendingPhotoUri);
        } catch (Exception ignored) {
            // Ignore cleanup issues after a canceled capture.
        }
    }

    private void loadImages() {
        imageItems.clear();

        if (selectedFolderUri == null) {
            imageAdapter.setItems(imageItems);
            showEmptyState(true);
            return;
        }

        DocumentFile folder = DocumentFile.fromTreeUri(this, selectedFolderUri);
        if (folder == null || !folder.exists()) {
            Toast.makeText(this, R.string.folder_not_available, Toast.LENGTH_SHORT).show();
            showEmptyState(true);
            return;
        }

        ContentResolver resolver = getContentResolver();
        for (DocumentFile file : folder.listFiles()) {
            if (!isImageFile(file)) {
                continue;
            }

            String name = file.getName() != null ? file.getName() : getString(R.string.unnamed_image);
            long size = file.length();
            long lastModified = file.lastModified();
            String dateTaken = ImageMetadataUtils.resolveDateTaken(resolver, file.getUri(), lastModified);
            String readableImagePath = ImageMetadataUtils.resolveReadablePath(file.getUri());
            String readableFolderPath = ImageMetadataUtils.parentPath(readableImagePath);
            String folderName = ImageMetadataUtils.leafName(readableFolderPath);
            String displayName = ImageMetadataUtils.buildFriendlyName(name);
            String galleryMeta = ImageMetadataUtils.formatCompactDate(lastModified);

            imageItems.add(new ImageItem(
                    file.getUri(),
                    name,
                    displayName,
                    folderName,
                    readableFolderPath,
                    galleryMeta,
                    size,
                    lastModified,
                    dateTaken
            ));
        }

        imageItems.sort(Comparator.comparingLong(ImageItem::getLastModified).reversed());
        imageAdapter.setItems(imageItems);
        showEmptyState(imageItems.isEmpty());
    }

    private boolean isImageFile(DocumentFile file) {
        if (file == null || !file.isFile()) {
            return false;
        }

        String type = file.getType();
        return type != null && type.startsWith("image/");
    }

    private void showEmptyState(boolean show) {
        emptyStateLabel.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onImageClicked(ImageItem imageItem) {
        Intent intent = new Intent(this, ImageDetailActivity.class);
        intent.putExtra(ImageDetailActivity.EXTRA_URI, imageItem.getUri().toString());
        intent.putExtra(ImageDetailActivity.EXTRA_DISPLAY_NAME, imageItem.getDisplayName());
        intent.putExtra(ImageDetailActivity.EXTRA_FILE_NAME, imageItem.getFileName());
        intent.putExtra(ImageDetailActivity.EXTRA_FOLDER_NAME, imageItem.getFolderName());
        intent.putExtra(ImageDetailActivity.EXTRA_LOCATION_PATH, imageItem.getLocationPath());
        intent.putExtra(ImageDetailActivity.EXTRA_SIZE, imageItem.getSize());
        intent.putExtra(ImageDetailActivity.EXTRA_DATE_TAKEN, imageItem.getDateTaken());
        detailLauncher.launch(intent);
    }
}
