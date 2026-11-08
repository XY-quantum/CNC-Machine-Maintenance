package cnc.woodcut.machine.maintenance.ui.machines;

import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import cnc.woodcut.machine.maintenance.R;

/**
 * Machine Schedule Calendar.
 *
 * Displays a machine list with pictures and stores one memo file
 * for each machine and selected date.
 */
public class CalendarFragment extends Fragment {

    private static final String TAG = "CalendarMemo";
    private static final String MEMO_FOLDER_NAME = "machine_calendar_memos";
    private static final String MEMO_FILE_PREFIX = "memo_";

    /*
     * Layout views.
     */
    private CalendarView calendarView;
    private TextView txtSelectedDate;
    private TextView txtMachineName;
    private EditText editMemo;
    private Button btnSaveMemo;
    private Button btnDeleteMemo;
    private Spinner spinnerMachine;
    private ImageView imageMachine;

    /*
     * Current machine and date selections.
     */
    private String selectedDateKey;
    private String selectedMachineId;
    private String selectedMachineName;

    /*
     * Folder containing the memo text files.
     */
    private File memoFolder;

    /*
     * Machine names displayed in the Spinner.
     */
    private final String[] machineNames = {
            "CNC Machine 1",
            "CNC Machine 2",
            "CNC Machine 3"
    };

    /*
     * Stable machine IDs used in memo filenames.
     */
    private final String[] machineIds = {
            "cnc_machine_1",
            "cnc_machine_2",
            "cnc_machine_3"
    };

    /*
     * Machine pictures from res/drawable.
     *
     * Required drawable files:
     * cnc_machine_1.jpg
     * cnc_machine_2.jpg
     * cnc_machine_3.jpg
     */
    private final int[] machinePictures = {
            R.drawable.cnc_machine_1,
            R.drawable.cnc_machine_2,
            R.drawable.cnc_machine_3
    };

    public CalendarFragment() {
        super(R.layout.fragment_calendar);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        /*
         * Connect Java fields to the views in fragment_calendar.xml.
         */
        calendarView = view.findViewById(R.id.calendarView);
        txtSelectedDate = view.findViewById(R.id.txtSelectedDate);
        txtMachineName = view.findViewById(R.id.txtMachineName);
        editMemo = view.findViewById(R.id.editMemo);
        btnSaveMemo = view.findViewById(R.id.btnSaveMemo);
        btnDeleteMemo = view.findViewById(R.id.btnDeleteMemo);
        spinnerMachine = view.findViewById(R.id.spinnerMachine);
        imageMachine = view.findViewById(R.id.imageMachine);

        /*
         * Create the memo folder first.
         */
        createMemoFolder();

        /*
         * Set today's date before configuring the machine Spinner.
         */
        setInitialDate(calendarView.getDate());

        /*
         * Configure machine names and pictures.
         */
        configureMachineSpinner();

        /*
         * Change the selected date and load that date's memo.
         */
        calendarView.setOnDateChangeListener(
                (calendar, year, month, dayOfMonth) -> {

                    // CalendarView uses zero-based months.
                    int actualMonth = month + 1;

                    selectedDateKey = String.format(
                            Locale.US,
                            "%04d-%02d-%02d",
                            year,
                            actualMonth,
                            dayOfMonth
                    );

                    String displayedDate = String.format(
                            Locale.getDefault(),
                            "Selected date: %02d/%02d/%04d",
                            dayOfMonth,
                            actualMonth,
                            year
                    );

                    txtSelectedDate.setText(displayedDate);

                    /*
                     * Load the memo for the selected machine and date.
                     */
                    loadMemo();
                }
        );

        /*
         * Save and delete button listeners.
         */
        btnSaveMemo.setOnClickListener(v -> saveMemo());
        btnDeleteMemo.setOnClickListener(v -> deleteMemo());
    }

    /**
     * Configures the machine Spinner.
     *
     * Selecting a machine changes:
     * 1. Machine name
     * 2. Machine ID
     * 3. Machine picture
     * 4. Loaded memo
     */
    private void configureMachineSpinner() {

        /*
         * All machine arrays must have the same number of entries.
         */
        if (machineNames.length != machineIds.length
                || machineNames.length != machinePictures.length) {

            Log.e(
                    TAG,
                    "Machine names, IDs, and pictures have different lengths"
            );

            Toast.makeText(
                    requireContext(),
                    "Machine configuration is invalid",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        ArrayAdapter<String> machineAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                machineNames
        );

        machineAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerMachine.setAdapter(machineAdapter);

        spinnerMachine.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View selectedView,
                            int position,
                            long id
                    ) {
                        if (position < 0
                                || position >= machineNames.length) {
                            return;
                        }

                        /*
                         * Update selected machine information.
                         */
                        selectedMachineName = machineNames[position];
                        selectedMachineId = machineIds[position];

                        /*
                         * Display selected machine name.
                         */
                        txtMachineName.setText(selectedMachineName);

                        /*
                         * Display selected machine picture.
                         */
                        imageMachine.setImageResource(
                                machinePictures[position]
                        );

                        imageMachine.setContentDescription(
                                selectedMachineName + " picture"
                        );

                        Log.d(
                                TAG,
                                "Selected machine: "
                                        + selectedMachineName
                                        + ", ID: "
                                        + selectedMachineId
                        );

                        /*
                         * Load the selected machine's memo for the
                         * currently selected date.
                         */
                        if (selectedDateKey != null) {
                            loadMemo();
                        }
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent
                    ) {
                        // Keep the previous machine selection.
                    }
                }
        );

        /*
         * Select CNC Machine 1 when the screen first opens.
         */
        spinnerMachine.setSelection(0);
    }

    /**
     * Creates the app-specific external memo folder.
     *
     * Expected folder:
     *
     * Android/data/cnc.woodcut.machine.maintenance/
     * files/Documents/calendar_memos/
     */
    private void createMemoFolder() {

        File documentsFolder =
                requireContext().getExternalFilesDir(
                        Environment.DIRECTORY_DOCUMENTS
                );

        if (documentsFolder == null) {
            Log.e(TAG, "External storage is unavailable");

            Toast.makeText(
                    requireContext(),
                    "External storage is unavailable",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        memoFolder = new File(
                documentsFolder,
                MEMO_FOLDER_NAME
        );

        /*
         * Create the folder if it does not already exist.
         */
        if (!memoFolder.exists()) {
            boolean created = memoFolder.mkdirs();

            if (!created && !memoFolder.exists()) {
                Log.e(
                        TAG,
                        "Unable to create memo folder: "
                                + memoFolder.getAbsolutePath()
                );

                Toast.makeText(
                        requireContext(),
                        "Unable to create memo folder",
                        Toast.LENGTH_LONG
                ).show();

                memoFolder = null;
                return;
            }
        }

        Log.d(
                TAG,
                "Memo folder: " + memoFolder.getAbsolutePath()
        );
    }

    /**
     * Selects today's date when the fragment opens.
     */
    private void setInitialDate(long dateInMilliseconds) {

        Date date = new Date(dateInMilliseconds);

        SimpleDateFormat keyFormat =
                new SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.US
                );

        SimpleDateFormat displayFormat =
                new SimpleDateFormat(
                        "EEEE, d MMMM yyyy",
                        Locale.getDefault()
                );

        selectedDateKey = keyFormat.format(date);
        txtSelectedDate.setText(displayFormat.format(date));

        /*
         * The machine has not necessarily been selected yet.
         * getMemoFile() safely handles a null machine ID.
         */
        loadMemo();
    }

    /**
     * Returns the memo file for the selected machine and date.
     *
     * Example:
     * memo_cnc_machine_1_2026-11-06.txt
     */
    @Nullable
    private File getMemoFile() {

        if (memoFolder == null
                || selectedMachineId == null
                || selectedDateKey == null) {

            return null;
        }

        String fileName =
                MEMO_FILE_PREFIX
                        + selectedMachineId
                        + "_"
                        + selectedDateKey
                        + ".txt";

        return new File(memoFolder, fileName);
    }

    /**
     * Loads the memo for the selected machine and date.
     */
    private void loadMemo() {

        File memoFile = getMemoFile();

        if (memoFile == null || !memoFile.exists()) {
            editMemo.setText("");
            btnDeleteMemo.setEnabled(false);

            Log.d(
                    TAG,
                    "No memo found for machine "
                            + selectedMachineName
                            + " on "
                            + selectedDateKey
            );

            return;
        }

        /*
         * Avoid loading unexpectedly large files into memory.
         */
        if (memoFile.length() > 1_000_000) {
            Log.e(TAG, "Memo file is unexpectedly large");

            Toast.makeText(
                    requireContext(),
                    "Memo file is too large",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        try (FileInputStream inputStream =
                     new FileInputStream(memoFile)) {

            byte[] fileBytes =
                    new byte[(int) memoFile.length()];

            int totalBytesRead = 0;
            int bytesRead;

            while (totalBytesRead < fileBytes.length
                    && (bytesRead = inputStream.read(
                    fileBytes,
                    totalBytesRead,
                    fileBytes.length - totalBytesRead
            )) != -1) {

                totalBytesRead += bytesRead;
            }

            String fileContent = new String(
                    fileBytes,
                    0,
                    totalBytesRead,
                    StandardCharsets.UTF_8
            );

            /*
             * Extract only the memo text from the stored file.
             */
            String memo = extractMemoText(fileContent);

            editMemo.setText(memo);
            editMemo.setSelection(memo.length());
            btnDeleteMemo.setEnabled(true);

            Log.d(
                    TAG,
                    "Memo loaded: " + memoFile.getAbsolutePath()
            );

        } catch (IOException exception) {
            Log.e(TAG, "Unable to load memo", exception);

            Toast.makeText(
                    requireContext(),
                    "Unable to load memo",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    /**
     * Extracts the memo text from the stored file.
     *
     * It also supports older memo files that contain only plain text.
     */
    private String extractMemoText(String fileContent) {

        String separator = "\nMemo:\n";
        int separatorPosition =
                fileContent.indexOf(separator);

        if (separatorPosition == -1) {
            return fileContent;
        }

        return fileContent.substring(
                separatorPosition + separator.length()
        );
    }

    /**
     * Saves a memo for the selected machine and date.
     */
    private void saveMemo() {

        String memo =
                editMemo.getText().toString().trim();

        if (memo.isEmpty()) {
            editMemo.setError("Enter a memo");
            editMemo.requestFocus();
            return;
        }

        if (selectedMachineId == null) {
            Toast.makeText(
                    requireContext(),
                    "Select a machine",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (selectedDateKey == null) {
            Toast.makeText(
                    requireContext(),
                    "Select a date",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (memoFolder == null) {
            createMemoFolder();
        }

        File memoFile = getMemoFile();

        if (memoFile == null) {
            Toast.makeText(
                    requireContext(),
                    "Memo file is unavailable",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        /*
         * Store readable machine and date information in the file.
         */
        String fileContent =
                "Machine: " + selectedMachineName + "\n"
                        + "Machine ID: " + selectedMachineId + "\n"
                        + "Date: " + selectedDateKey + "\n"
                        + "Memo:\n"
                        + memo;

        try (FileOutputStream outputStream =
                     new FileOutputStream(memoFile, false)) {

            outputStream.write(
                    fileContent.getBytes(
                            StandardCharsets.UTF_8
                    )
            );

            outputStream.flush();

            btnDeleteMemo.setEnabled(true);

            Log.d(
                    TAG,
                    "Memo saved: " + memoFile.getAbsolutePath()
            );

            Toast.makeText(
                    requireContext(),
                    "Memo saved for "
                            + selectedMachineName
                            + " on "
                            + selectedDateKey,
                    Toast.LENGTH_SHORT
            ).show();

        } catch (IOException exception) {
            Log.e(TAG, "Unable to save memo", exception);

            Toast.makeText(
                    requireContext(),
                    "Unable to save memo",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    /**
     * Deletes the memo for the selected machine and date.
     */
    private void deleteMemo() {

        File memoFile = getMemoFile();

        if (memoFile == null || !memoFile.exists()) {
            editMemo.setText("");
            btnDeleteMemo.setEnabled(false);

            Toast.makeText(
                    requireContext(),
                    "No memo exists for this machine and date",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        boolean deleted = memoFile.delete();

        if (deleted) {
            editMemo.setText("");
            btnDeleteMemo.setEnabled(false);

            Log.d(
                    TAG,
                    "Memo deleted: " + memoFile.getAbsolutePath()
            );

            Toast.makeText(
                    requireContext(),
                    "Memo deleted",
                    Toast.LENGTH_SHORT
            ).show();

        } else {
            Log.e(
                    TAG,
                    "Unable to delete memo: "
                            + memoFile.getAbsolutePath()
            );

            Toast.makeText(
                    requireContext(),
                    "Unable to delete memo",
                    Toast.LENGTH_LONG
            ).show();
        }
    }
}