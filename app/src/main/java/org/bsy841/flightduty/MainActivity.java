package org.bsy841.flightduty;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;

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

    // 输入控件：TextInputLayout + MaterialAutoCompleteTextView 实现下拉选择
    private TextInputLayout crewInput;
    private AutoCompleteTextView crewDropdown;
    private TextInputLayout legInput;
    private AutoCompleteTextView legDropdown;
    private TextInputLayout restInput;
    private AutoCompleteTextView restDropdown;

    private TextInputLayout hourInput;
    private AutoCompleteTextView hourDropdown;
    private TextInputLayout minuteInput;
    private AutoCompleteTextView minuteDropdown;

    private TextInputLayout restHourInput;
    private AutoCompleteTextView restHourDropdown;
    private TextInputLayout restMinuteInput;
    private AutoCompleteTextView restMinuteDropdown;

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
        initDropdowns();
        initListeners();
    }

    /** 绑定所有视图控件 */
    private void bindViews() {
        crewInput = findViewById(R.id.crewInput);
        crewDropdown = findViewById(R.id.crewDropdown);
        legInput = findViewById(R.id.legInput);
        legDropdown = findViewById(R.id.legDropdown);
        restInput = findViewById(R.id.restInput);
        restDropdown = findViewById(R.id.restDropdown);

        hourInput = findViewById(R.id.hourInput);
        hourDropdown = findViewById(R.id.hourDropdown);
        minuteInput = findViewById(R.id.minuteInput);
        minuteDropdown = findViewById(R.id.minuteDropdown);

        restHourInput = findViewById(R.id.restHourInput);
        restHourDropdown = findViewById(R.id.restHourDropdown);
        restMinuteInput = findViewById(R.id.restMinuteInput);
        restMinuteDropdown = findViewById(R.id.restMinuteDropdown);

        resultText = findViewById(R.id.resultText);
        legLayout = findViewById(R.id.legLayout);
        restLayout = findViewById(R.id.restLayout);
        calcButton = findViewById(R.id.calcButton);
    }

    /** 初始化所有下拉菜单的数据与默认选中项 */
    private void initDropdowns() {
        // 机组配置
        setDropdown(crewDropdown, listOf("非扩编组", "扩编组（3人）", "扩编组（4人）"), "非扩编组");
        crewDropdown.setOnItemClickListener((parent, view, position, id) -> onCrewChange(position));

        // 航段数
        setDropdown(legDropdown, listOf("1-4段", "5段", "6段", "7段"), "1-4段");

        // 休息设施
        setDropdown(restDropdown, listOf("1级休息设施", "2级休息设施", "3级休息设施"), "1级休息设施");

        // 起飞时间
        setDropdown(hourDropdown, generateNumbers(0, 23), "09");
        setDropdown(minuteDropdown, generateNumbers(0, 59), "00");

        // 休息间隔
        setDropdown(restHourDropdown, generateNumbers(0, 23), "00");
        setDropdown(restMinuteDropdown, generateNumbers(0, 59), "00");

        onCrewChange(0);
    }

    /** 为 AutoCompleteTextView 设置数据并指定默认值 */
    private void setDropdown(AutoCompleteTextView view, List<String> items, String defaultValue) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, items);
        view.setAdapter(adapter);
        view.setText(defaultValue, false); // false = 不触发过滤动画
    }

    /** 生成带前导零的数字字符串列表（如 00, 01, ... 59） */
    private List<String> generateNumbers(int start, int end) {
        List<String> list = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            list.add(String.format("%02d", i));
        }
        return list;
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
        int takeoffHour = Integer.parseInt(hourDropdown.getText().toString());
        int takeoffMinute = Integer.parseInt(minuteDropdown.getText().toString());
        int takeoffMin = takeoffHour * 60 + takeoffMinute;
        String takeoffStr = String.format("%02d:%02d", takeoffHour, takeoffMinute);
        String takeoffHhmm = String.format("%02d%02d", takeoffHour, takeoffMinute);

        int restHour = Integer.parseInt(restHourDropdown.getText().toString());
        int restMinute = Integer.parseInt(restMinuteDropdown.getText().toString());
        int restIntervalMin = restHour * 60 + restMinute;

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
            sb.append("休息间隔：").append(String.format("%02d:%02d", restHour, restMinute)).append("\n");
        }
        sb.append("\n");
        sb.append("最大飞行时间：").append(maxFlt).append(" 小时\n");
        sb.append("最大执勤时间：").append(maxDuty).append(" 小时\n");
        sb.append("\n");
        sb.append("执勤超时临界时间：").append(RulesEngine.minutesToHHmmWithDay(dutyCritical)).append("\n");
        if (restIntervalMin > 0) {
            sb.append("含休息间隔的执勤临界时间：").append(RulesEngine.minutesToHHmmWithDay(dutyCriticalWithRest)).append("\n");
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
     * 注意：不能依赖 Activity 实例变量保存状态，因此直接从 AppCompatDelegate 读取当前模式。
     */
    private void toggleTheme() {
        int currentMode = AppCompatDelegate.getDefaultNightMode();
        if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
        recreate();
    }
}
