package cnc.woodcut.machine.maintenance.ui.dashboard;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cnc.woodcut.machine.maintenance.R;


/**
 *  Maintenance Dashboard: Preventative Maintenance Plan, Calendar-based Schedule,
 *                         Notification for upcoming maintenance, Alarm to manager,
 *                         Analytics for machine uptime/downtime
 *                         Sync with cloud or local database(eg, Firebase, MySQL)
 */

public class DashboardFragment extends Fragment {
    private TextView statusText;
    private ProgressBar loadingIndicator;
    // Machine running data
    private BarChart barChart;
    private TextView averageTextView;
    Button btnMonthly;

    public DashboardFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        statusText = view.findViewById(R.id.statusText);
        loadingIndicator = view.findViewById(R.id.loadingIndicator);
        Button refreshButton = view.findViewById(R.id.refreshButton);

        refreshButton.setOnClickListener(v -> updateDashboardData());

        //Machine running data
        barChart = view.findViewById(R.id.barChart);
        averageTextView = view.findViewById(R.id.averageTextView);

        List<RunningSession> weeklyData = getWeeklyMockData();
        int weeklyAverage = calculateWeeklyAverage(weeklyData);
        averageTextView.setText("MachineX Weekly Average Running: " + weeklyAverage + " min");

        //setupChart(weeklyData, weeklyAverage);
        // Load default view
        updateChart(getWeeklyMockData(), "Weekly");
        btnMonthly = view.findViewById(R.id.btnMonthly);
        btnMonthly.setOnClickListener(v -> {
            List<RunningSession> monthlyData = getMonthlyMockData();
            updateChart(monthlyData, "MachineX Monthly");
        });

    }
    private void updateChart(List<RunningSession> sessions, String label) {
        int average = calculateWeeklyAverage(sessions); // works for monthly too
        averageTextView.setText(label + " Average Running time: " + average + " min");

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < sessions.size(); i++) {
            entries.add(new BarEntry(i, sessions.get(i).getMinutesRun()));
            labels.add(sessions.get(i).getDayOfWeek());
        }

        BarDataSet dataSet = new BarDataSet(entries, label + " Running Time");
        dataSet.setColor(Color.BLUE);

        BarData barData = new BarData(dataSet);
        barChart.setData(barData);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setLabelRotationAngle(labels.size() > 7 ? -45 : 0); // Rotate if many labels
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.removeAllLimitLines();
        LimitLine avgLine = new LimitLine(average, label + " Avg");
        avgLine.setLineColor(Color.RED);
        avgLine.setLineWidth(2f);
        leftAxis.addLimitLine(avgLine);

        barChart.getDescription().setEnabled(false);
        barChart.invalidate();
    }
    private List<RunningSession> getWeeklyMockData() {
        return Arrays.asList(
                new RunningSession("Monday", 30),
                new RunningSession("Tuesday", 45),
                new RunningSession("Wednesday", 20),
                new RunningSession("Thursday", 50),
                new RunningSession("Friday", 35),
                new RunningSession("Saturday", 60),
                new RunningSession("Sunday", 40)
        );
    }

    private List<RunningSession> getMonthlyMockData() {
        List<RunningSession> data = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            data.add(new RunningSession("Day " + i, (int)(Math.random() * 60)));
        }
        return data;
    }

    private int calculateWeeklyAverage(List<RunningSession> sessions) {
        int total = 0;
        for (RunningSession session : sessions) {
            total += session.getMinutesRun();
        }
        return total / sessions.size();
    }

    private void setupChart(List<RunningSession> sessions, int average) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < sessions.size(); i++) {
            entries.add(new BarEntry(i, sessions.get(i).getMinutesRun()));
            labels.add(sessions.get(i).getDayOfWeek());
        }

        BarDataSet dataSet = new BarDataSet(entries, "Daily Running Time");
        dataSet.setColor(Color.BLUE);

        BarData barData = new BarData(dataSet);
        barChart.setData(barData);

        // Add average line
        LimitLine avgLine = new LimitLine(average, "Weekly Avg");
        avgLine.setLineColor(Color.RED);
        avgLine.setLineWidth(2f);
        avgLine.setTextColor(Color.RED);
        avgLine.setTextSize(12f);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.addLimitLine(avgLine);

        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChart.getXAxis().setGranularity(1f);
        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChart.getDescription().setEnabled(false);
        barChart.invalidate(); // refresh

        barChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                int index = (int) e.getX();
                float value = e.getY();
                String day = sessions.get(index).getDayOfWeek();
                Toast.makeText(getContext(), day + ": " + value + " min", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected() {
                // Optional: handle deselection
            }
        });
    }
    private void updateDashboardData() {
        loadingIndicator.setVisibility(View.VISIBLE);
        statusText.setText("Updating...");

        // Simulate data fetch with a delay
        new Handler().postDelayed(() -> {
            // Example updated data
            String updatedStatus = "Machines Running Status: All machines operational";

            statusText.setText(updatedStatus);
            loadingIndicator.setVisibility(View.GONE);
        }, 2000); // 2-second delay to simulate network call
    }

    private class RunningSession {
        private String dayOfWeek;
        private int minutesRun;

        public RunningSession(String dayOfWeek, int minutesRun) {
            this.dayOfWeek = dayOfWeek;
            this.minutesRun = minutesRun;
        }

        public String getDayOfWeek() {
            return dayOfWeek;
        }

        public int getMinutesRun() {
            return minutesRun;
        }
    }

}