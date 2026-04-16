package org.bsy841.flightduty;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;

/**
 * 主界面 Activity。
 * 采用 Material Design 3 组件构建输入表单与结果展示，支持日间/夜间主题切换。
 */
public class MainActivity extends AppCompatActivity {

    // 用于在 recreate() 时保存状态（静态变量在 Activity 重建后仍然保留）
    private static String savedCrewSelection = null;
    private static String savedLegSelection = null;
    private static String savedRestSelection = null;

    // 下拉选择控件（MaterialAutoCompleteTextView）
    private MaterialAutoCompleteTextView crewDropdown;
    private MaterialAutoCompleteTextView legDropdown;
    private MaterialAutoCompleteTextView restDropdown;

    // 时间滚轮选择器（NumberPicker）
    private NumberPicker hourPicker;
    private NumberPicker minutePicker;
    private NumberPicker restHourPicker;
    private NumberPicker restMinutePicker;

    // 结果与按钮
    private MaterialTextView resultText;
    private LinearLayout legLayout;
    private LinearLayout restLayout;
    private MaterialButton calcButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 设置 ActionBar 标题
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("飞行机组执勤时间计算器");
        }

        bindViews();
        initPickers();
        initDropdowns(savedInstanceState);
        initListeners();
    }

    /** 绑定所有视图控件 */
    private void bindViews() {
        crewDropdown = findViewById(R.id.crewDropdown);
        legDropdown = findViewById(R.id.legDropdown);
        restDropdown = findViewById(R.id.restDropdown);

        hourPicker = findViewById(R.id.hourPicker);
        minutePicker = findViewById(R.id.minutePicker);
        restHourPicker = findViewById(R.id.restHourPicker);
        restMinutePicker = findViewById(R.id.restMinutePicker);

        resultText = findViewById(R.id.resultText);
        legLayout = findViewById(R.id.legLayout);
        restLayout = findViewById(R.id.restLayout);
        calcButton = findViewById(R.id.calcButton);
    }

    /** 初始化 NumberPicker（小时/分钟滚轮） */
    private void initPickers() {
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

    /**
     * 初始化下拉菜单数据与默认选中项。
     * 如果是 Activity 重建（如主题切换），使用静态变量恢复之前的选择。
     */
    private void initDropdowns(Bundle savedInstanceState) {
        // 判断是否有保存的状态（主题切换时）
        boolean hasSavedState = savedCrewSelection != null;
        
        // 机组配置
        String crewDefault = hasSavedState ? savedCrewSelection : "非扩编组";
        setDropdown(crewDropdown, listOf("非扩编组", "扩编组（3人）", "扩编组（4人）"), crewDefault);
        crewDropdown.setOnItemClickListener((parent, view, position, id) -> {
            onCrewChange(position);
            // 更新保存的状态
            savedCrewSelection = crewDropdown.getText().toString();
        });

        // 航段数
        String legDefault = hasSavedState ? savedLegSelection : "1-4段";
        setDropdown(legDropdown, listOf("1-4段", "5段", "6段", "7段"), legDefault);
        legDropdown.setOnItemClickListener((parent, view, position, id) -> {
            savedLegSelection = legDropdown.getText().toString();
        });

        // 休息设施
        String restDefault = hasSavedState ? savedRestSelection : "1级休息设施";
        setDropdown(restDropdown, listOf("1级休息设施", "2级休息设施", "3级休息设施"), restDefault);
        restDropdown.setOnItemClickListener((parent, view, position, id) -> {
            savedRestSelection = restDropdown.getText().toString();
        });

        // 根据当前选中的机组配置，恢复界面状态
        int position = crewDefault.equals("非扩编组") ? 0 
                : crewDefault.equals("扩编组（3人）") ? 1 : 2;
        onCrewChange(position);
        
        // 注意：不清除静态变量，确保连续切换主题时状态能持续保存
        // 静态变量会在应用进程被杀死时自动清除
    }

    /** 
     * 为 MaterialAutoCompleteTextView 设置数据并指定默认值。
     * Activity 重建时会自动恢复文本，需先清除再设置避免冲突。
     */
    private void setDropdown(MaterialAutoCompleteTextView view, List<String> items, String defaultValue) {
        // 先清除文本（防止 Android 自动恢复的旧文本干扰）
        view.setText("", false);
        
        // 设置数据适配器
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, items);
        view.setAdapter(adapter);
        
        // 设置默认值（false = 不触发过滤）
        if (defaultValue != null && !defaultValue.isEmpty()) {
            view.setText(defaultValue, false);
        }
    }

    private List<String> listOf(String... items) {
        List<String> list = new ArrayList<>();
        for (String item : items) list.add(item);
        return list;
    }

    /** 初始化按钮监听器 */
    private void initListeners() {
        calcButton.setOnClickListener(v -> calculate());
    }

    /**
     * 根据机组配置切换输入项的可见性。
     * 非扩编组显示"航段数"，扩编组显示"休息设施"。
     */
    private void onCrewChange(int position) {
        if (position == 0) { // 非扩编组
            legLayout.setVisibility(LinearLayout.VISIBLE);
            restLayout.setVisibility(LinearLayout.GONE);
        } else {
            legLayout.setVisibility(LinearLayout.GONE);
            restLayout.setVisibility(LinearLayout.VISIBLE);
        }
    }

    /**
     * 核心计算逻辑。
     * 步骤：
     * 1. 解析用户输入的起飞时间与休息间隔；
     * 2. 根据起飞时间查表得到最大飞行时间和最大执勤时间；
     * 3. 计算临界时间（含休息间隔累加）；
     * 4. 将结果渲染到结果卡片中，时间超过24小时显示为 (+N) 格式。
     */
    private void calculate() {
        // 1. 解析输入
        int takeoffMin = hourPicker.getValue() * 60 + minutePicker.getValue();
        String takeoffStr = String.format("%02d:%02d", hourPicker.getValue(), minutePicker.getValue());
        String takeoffHhmm = String.format("%02d%02d", hourPicker.getValue(), minutePicker.getValue());

        int restIntervalMin = restHourPicker.getValue() * 60 + restMinutePicker.getValue();

        String crewText = crewDropdown.getText().toString();
        String peopleKey = RulesEngine.PEOPLE_MAP.get(crewText);

        // 2. 查最大飞行时间
        Integer maxFlt = RulesEngine.lookupValue("flt", peopleKey, takeoffHhmm, null, null);
        if (maxFlt == null) {
            resultText.setText("错误：未找到对应的飞行时间规则");
            resultText.setTextColor(getColor(android.R.color.holo_red_dark));
            return;
        }

        // 3. 查最大执勤时间
        Integer maxDuty;
        if ("非扩编".equals(peopleKey)) {
            String legText = legDropdown.getText().toString();
            String legKey = RulesEngine.LEG_MAP.get(legText);
            maxDuty = RulesEngine.lookupValue("duty", peopleKey, takeoffHhmm, legKey, null);
        } else {
            String restText = restDropdown.getText().toString();
            maxDuty = RulesEngine.lookupValue("duty", peopleKey, takeoffHhmm, null, restText);
        }

        if (maxDuty == null) {
            resultText.setText("错误：未找到对应的执勤时间规则");
            resultText.setTextColor(getColor(android.R.color.holo_red_dark));
            return;
        }

        // 4. 计算临界时间（分钟）
        int dutyCritical = takeoffMin + maxDuty * 60;
        int dutyCriticalWithRest = dutyCritical + restIntervalMin;

        // 5. 组装结果文本
        StringBuilder sb = new StringBuilder();
        sb.append("机组配置：").append(crewText).append("\n");
        sb.append("起飞时间：").append(takeoffStr).append("\n");
        if ("非扩编".equals(peopleKey)) {
            sb.append("航段数：").append(legDropdown.getText().toString()).append("\n");
        } else {
            sb.append("休息设施：").append(restDropdown.getText().toString()).append("\n");
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
        sb.append("执勤超时临界时间：").append(RulesEngine.minutesToHHmmWithDay(dutyCritical)).append("\n");
        if (restIntervalMin > 0) {
            sb.append("含休息间隔的执勤临界时间：")
              .append(RulesEngine.minutesToHHmmWithDay(dutyCriticalWithRest))
              .append("\n");
        }
        sb.append("\n");
        sb.append("说明：临界时间 = 起飞时间 + 对应时限；若填写了休息间隔，则额外累加至执勤临界时间中。");

        // 根据当前主题设置合适的文字颜色，确保夜间模式下结果文字清晰可见
        boolean isNight = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        resultText.setTextColor(getColor(isNight ? android.R.color.white : android.R.color.black));
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

    /**
     * 切换日间/夜间主题。
     * 通过 AppCompatDelegate 设置全局 NightMode，然后调用 recreate() 重建 Activity。
     * 重建前先保存当前选择，重建后在 initDropdowns() 中恢复。
     */
    private void toggleTheme() {
        // 保存当前选择到静态变量
        if (crewDropdown != null) {
            savedCrewSelection = crewDropdown.getText().toString();
            savedLegSelection = legDropdown.getText().toString();
            savedRestSelection = restDropdown.getText().toString();
        }
        
        int currentMode = AppCompatDelegate.getDefaultNightMode();
        if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
        recreate();
    }
}
