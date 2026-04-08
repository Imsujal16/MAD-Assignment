package com.mad.cameragallery;

import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

public class ImageDetailActivity extends AppCompatActivity {
    public static final String EXTRA_URI = "extra_uri";
    public static final String EXTRA_DISPLAY_NAME = "extra_display_name";
    public static final String EXTRA_FILE_NAME = "extra_file_name";
    public static final String EXTRA_FOLDER_NAME = "extra_folder_name";
    public static final String EXTRA_LOCATION_PATH = "extra_location_path";
    public static final String EXTRA_SIZE = "extra_size";
    public static final String EXTRA_DATE_TAKEN = "extra_date_taken";

    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_detail);

        imageUri = Uri.parse(getIntent().getStringExtra(EXTRA_URI));
        String displayName = getIntent().getStringExtra(EXTRA_DISPLAY_NAME);
        String fileName = getIntent().getStringExtra(EXTRA_FILE_NAME);
        String folderName = getIntent().getStringExtra(EXTRA_FOLDER_NAME);
        String locationPath = getIntent().getStringExtra(EXTRA_LOCATION_PATH);
        long imageSize = getIntent().getLongExtra(EXTRA_SIZE, -1L);
        String dateTaken = getIntent().getStringExtra(EXTRA_DATE_TAKEN);

        bindContent(displayName, fileName, folderName, locationPath, imageSize, dateTaken);
        setupActions();
    }

    private void bindContent(
            String displayName,
            String fileName,
            String folderName,
            String locationPath,
            long imageSize,
            String dateTaken
    ) {
        ImageView previewImage = findViewById(R.id.previewImage);
        TextView titleValue = findViewById(R.id.nameValue);
        TextView fileNameValue = findViewById(R.id.fileNameValue);
        TextView folderValue = findViewById(R.id.folderValue);
        TextView pathValue = findViewById(R.id.pathValue);
        TextView sizeValue = findViewById(R.id.sizeValue);
        TextView dateValue = findViewById(R.id.dateValue);

        Glide.with(this)
                .load(imageUri)
                .centerInside()
                .into(previewImage);

        titleValue.setText(displayName != null ? displayName : getString(R.string.unnamed_image));
        fileNameValue.setText(fileName != null ? fileName : getString(R.string.unnamed_image));
        folderValue.setText(folderName != null ? folderName : getString(R.string.selected_folder));
        pathValue.setText(locationPath != null ? locationPath : getString(R.string.unknown_value));
        sizeValue.setText(ImageMetadataUtils.formatFileSize(this, imageSize));
        dateValue.setText(dateTaken != null ? dateTaken : getString(R.string.unknown_value));
    }

    private void setupActions() {
        ImageButton backButton = findViewById(R.id.backButton);
        MaterialButton deleteButton = findViewById(R.id.deleteButton);

        backButton.setOnClickListener(view -> finish());
        deleteButton.setOnClickListener(view -> showDeleteConfirmation());
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_dialog_title)
                .setMessage(R.string.delete_dialog_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.delete, this::deleteImage)
                .show();
    }

    private void deleteImage(DialogInterface dialog, int which) {
        try {
            boolean deleted = DocumentsContract.deleteDocument(getContentResolver(), imageUri);
            if (deleted) {
                Toast.makeText(this, R.string.image_deleted, Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, R.string.delete_failed, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception exception) {
            Toast.makeText(this, R.string.delete_failed, Toast.LENGTH_SHORT).show();
        }
    }
}
