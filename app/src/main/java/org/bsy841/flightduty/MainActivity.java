package org.bsy841.flightduty;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Spinner crewSpinner;
    private Spinner legSpinner;
    private Spinner restSpinner;
    private NumberPicker hourPicker;
    private NumberPicker minutePicker;
    private NumberPicker restHourPicker;
    private NumberPicker restMinutePicker;
    private TextView resultText;
    private LinearLayout legLayout;
    private LinearLayout restLayout;
    private Button calcButton;

    private boolean isDarkTheme = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("飞行机组执勤时间计算器");
        }

        crewSpinner = findViewById(R.id.crewSpinner);
        legSpinner = findViewById(R.id.legSpinner);
        restSpinner = findViewById(R.id.restSpinner);
        hourPicker = findViewById(R.id.hourPicker);
        minutePicker = findViewById(R.id.minutePicker);
        restHourPicker = findViewById(R.id.restHourPicker);
        restMinutePicker = findViewById(R.id.restMinutePicker);
        resultText = findViewById(R.id.resultText);
        legLayout = findViewById(R.id.legLayout);
        restLayout = findViewById(R.id.restLayout);
        calcButton = findViewById(R.id.calcButton);

        initTimePickers();
        initSpinners();

        calcButton.setOnClickListener(v -> calculate());
    }

    private void initTimePickers() {
        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(23);
        hourPicker.setValue(9);
        hourPicker.setFormatter(value -> String.format("%02d", value));

        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        minutePicker.setValue(0);
        minutePicker.setFormatter(value -> String.format("%02d", value));

        restHourPicker.setMinValue(0);
        restHourPicker.setMaxValue(23);
        restHourPicker.setValue(0);
        restHourPicker.setFormatter(value -> String.format("%02d", value));

        restMinutePicker.setMinValue(0);
        restMinutePicker.setMaxValue(59);
        restMinutePicker.setValue(0);
        restMinutePicker.setFormatter(value -> String.format("%02d", value));
    }

    private void initSpinners() {
        List<String> crewOptions = new ArrayList<>();
        crewOptions.add("非扩编组");
        crewOptions.add("扩编组（3人）");
        crewOptions.add("扩编组（4人）");
        ArrayAdapter<String> crewAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, crewOptions);
        crewAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        crewSpinner.setAdapter(crewAdapter);

        List<String> legOptions = new ArrayList<>();
        legOptions.add("1-4段");
        legOptions.add("5段");
        legOptions.add("6段");
        legOptions.add("7段");
        ArrayAdapter<String> legAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, legOptions);
        legAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        legSpinner.setAdapter(legAdapter);

        List<String> restOptions = new ArrayList<>();
        restOptions.add("1级休息设施");
        restOptions.add("2级休息设施");
        restOptions.add("3级休息设施");
        ArrayAdapter<String> restAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, restOptions);
        restAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        restSpinner.setAdapter(restAdapter);

        crewSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onCrewChange(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        onCrewChange(0);
    }

    private void onCrewChange(int position) {
        if (position == 0) { // 非扩编组
            legLayout.setVisibility(View.VISIBLE);
            restLayout.setVisibility(View.GONE);
        } else {
            legLayout.setVisibility(View.GONE);
            restLayout.setVisibility(View.VISIBLE);
        }
    }

    private void calculate() {
        int takeoffMin = hourPicker.getValue() * 60 + minutePicker.getValue();
        String takeoffStr = String.format("%02d:%02d", hourPicker.getValue(), minutePicker.getValue());
        int restIntervalMin = restHourPicker.getValue() * 60 + restMinutePicker.getValue();
        String takeoffHhmm = String.format("%02d%02d", hourPicker.getValue(), minutePicker.getValue());

        String crewText = crewSpinner.getSelectedItem().toString();
        String peopleKey = RulesEngine.PEOPLE_MAP.get(crewText);

        Integer maxFlt = RulesEngine.lookupValue("flt", peopleKey, takeoffHhmm, null, null);
        if (maxFlt == null) {
            resultText.setText("错误：未找到对应的飞行时间规则");
            resultText.setTextColor(getColor(android.R.color.holo_red_dark));
            return;
        }

        Integer maxDuty;
        if ("非扩编".equals(peopleKey)) {
            String legText = legSpinner.getSelectedItem().toString();
            String legKey = RulesEngine.LEG_MAP.get(legText);
            maxDuty = RulesEngine.lookupValue("duty", peopleKey, takeoffHhmm, legKey, null);
        } else {
            String restText = restSpinner.getSelectedItem().toString();
            maxDuty = RulesEngine.lookupValue("duty", peopleKey, takeoffHhmm, null, restText);
        }

        if (maxDuty == null) {
            resultText.setText("错误：未找到对应的执勤时间规则");
            resultText.setTextColor(getColor(android.R.color.holo_red_dark));
            return;
        }

        int fltCritical = takeoffMin + maxFlt * 60;
        int dutyCritical = takeoffMin + maxDuty * 60;
        int dutyCriticalWithRest = dutyCritical + restIntervalMin;

        StringBuilder sb = new StringBuilder();
        sb.append("机组配置：").append(crewText).append("\n");
        sb.append("起飞时间：").append(takeoffStr).append("\n");
        if ("非扩编".equals(peopleKey)) {
            sb.append("航段数：").append(legSpinner.getSelectedItem().toString()).append("\n");
        } else {
            sb.append("休息设施：").append(restSpinner.getSelectedItem().toString()).append("\n");
        }
        if (restIntervalMin > 0) {
            sb.append("休息间隔：")
              .append(String.format("%02d:%02d", restHourPicker.getValue(), restMinutePicker.getValue()))
              .append("\n");
        }
        sb.append("\n");
        sb.append("最大飞行时间：").append(maxFlt).append(" 小时\n");
        sb.append("最大执勤时间：").append(maxDuty).append(" 小时\n");
        sb.append("\n");
        sb.append("执勤超时临界时间：").append(RulesEngine.minutesToHHmm(dutyCritical)).append("\n");
        if (restIntervalMin > 0) {
            sb.append("含休息间隔的执勤临界时间：").append(RulesEngine.minutesToHHmm(dutyCriticalWithRest)).append("\n");
        }
        sb.append("\n");
        sb.append("说明：临界时间 = 起飞时间 + 对应时限；若填写了休息间隔，则额外累加至执勤临界时间中。");

        resultText.setTextColor(getColor(isDarkTheme ? android.R.color.white : android.R.color.black));
        resultText.setText(sb.toString());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_theme) {
            toggleTheme();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleTheme() {
        isDarkTheme = !isDarkTheme;
        if (isDarkTheme) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        recreate();
    }
}
