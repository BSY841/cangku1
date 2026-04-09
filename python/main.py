from kivy.metrics import dp
from kivy.uix.behaviors import ButtonBehavior
from kivy.clock import Clock
from kivy.graphics import Color, Line
from kivy.core.window import Window
from kivy.uix.floatlayout import FloatLayout
import os

from kivymd.app import MDApp
from kivymd.uix.screen import MDScreen
from kivymd.uix.toolbar import MDTopAppBar
from kivymd.uix.card import MDCard
from kivymd.uix.gridlayout import MDGridLayout
from kivymd.uix.boxlayout import MDBoxLayout
from kivymd.uix.scrollview import MDScrollView
from kivymd.uix.label import MDLabel
from kivymd.uix.button import MDRaisedButton, MDIconButton
from kivymd.uix.menu import MDDropdownMenu

# ============================================================
# 全局常量：中文字体路径与规则表
# ============================================================
FONT_PATH = "simhei.ttf"

# 查表规则（来自 time.xlsx）
RULES = [
    # 最大飞行时间
    {"cat": "flt", "people": "非扩编", "leg": None, "rest": None, "start": "0000", "end": "0459", "tm": 8},
    {"cat": "flt", "people": "非扩编", "leg": None, "rest": None, "start": "0500", "end": "1959", "tm": 9},
    {"cat": "flt", "people": "非扩编", "leg": None, "rest": None, "start": "2000", "end": "2359", "tm": 8},
    {"cat": "flt", "people": "加一人", "leg": None, "rest": None, "start": "0000", "end": "2359", "tm": 13},
    {"cat": "flt", "people": "加二人", "leg": None, "rest": None, "start": "0000", "end": "2359", "tm": 17},
    # 最大执勤时间 — 非扩编
    {"cat": "duty", "people": "非扩编", "leg": "4", "rest": None, "start": "0000", "end": "0459", "tm": 12},
    {"cat": "duty", "people": "非扩编", "leg": "5", "rest": None, "start": "0000", "end": "0459", "tm": 11},
    {"cat": "duty", "people": "非扩编", "leg": "6", "rest": None, "start": "0000", "end": "0459", "tm": 10},
    {"cat": "duty", "people": "非扩编", "leg": "7", "rest": None, "start": "0000", "end": "0459", "tm": 9},
    {"cat": "duty", "people": "非扩编", "leg": "4", "rest": None, "start": "0500", "end": "1159", "tm": 14},
    {"cat": "duty", "people": "非扩编", "leg": "5", "rest": None, "start": "0500", "end": "1159", "tm": 13},
    {"cat": "duty", "people": "非扩编", "leg": "6", "rest": None, "start": "0500", "end": "1159", "tm": 12},
    {"cat": "duty", "people": "非扩编", "leg": "7", "rest": None, "start": "0500", "end": "1159", "tm": 11},
    {"cat": "duty", "people": "非扩编", "leg": "4", "rest": None, "start": "1200", "end": "2359", "tm": 13},
    {"cat": "duty", "people": "非扩编", "leg": "5", "rest": None, "start": "1200", "end": "2359", "tm": 12},
    {"cat": "duty", "people": "非扩编", "leg": "6", "rest": None, "start": "1200", "end": "2359", "tm": 11},
    {"cat": "duty", "people": "非扩编", "leg": "7", "rest": None, "start": "1200", "end": "2359", "tm": 10},
    # 最大执勤时间 — 扩编组
    {"cat": "duty", "people": "加一人", "leg": None, "rest": "1级休息设施", "start": "0000", "end": "2359", "tm": 18},
    {"cat": "duty", "people": "加二人", "leg": None, "rest": "1级休息设施", "start": "0000", "end": "2359", "tm": 20},
    {"cat": "duty", "people": "加一人", "leg": None, "rest": "2级休息设施", "start": "0000", "end": "2359", "tm": 17},
    {"cat": "duty", "people": "加二人", "leg": None, "rest": "2级休息设施", "start": "0000", "end": "2359", "tm": 19},
    {"cat": "duty", "people": "加一人", "leg": None, "rest": "3级休息设施", "start": "0000", "end": "2359", "tm": 16},
    {"cat": "duty", "people": "加二人", "leg": None, "rest": "3级休息设施", "start": "0000", "end": "2359", "tm": 18},
]

PEOPLE_MAP = {
    "非扩编组": "非扩编",
    "扩编组（3人）": "加一人",
    "扩编组（4人）": "加二人",
}
LEG_MAP = {
    "1-4段": "4",
    "5段": "5",
    "6段": "6",
    "7段": "7",
}


def minutes_to_hhmm(total_minutes: int) -> str:
    """将分钟数转为 HH:MM"""
    h = total_minutes // 60
    m = total_minutes % 60
    return f"{h:02d}:{m:02d}"


def time_in_window(hhmm_str: str, start_str: str, end_str: str) -> bool:
    """判断 HHMM 字符串是否落在 [start, end] 闭区间内"""
    val = int(hhmm_str)
    start = int(start_str)
    end = int(end_str)
    return start <= val <= end


def lookup_value(cat: str, people_key: str, takeoff_hhmm: str, leg: str = None, rest: str = None) -> int:
    """从左往右竖着查表，返回 tm（小时数）"""
    for rule in RULES:
        if rule["cat"] != cat:
            continue
        if rule["people"] != people_key:
            continue
        if leg is not None and rule.get("leg") != leg:
            continue
        if rest is not None and rule.get("rest") != rest:
            continue
        if time_in_window(takeoff_hhmm, rule["start"], rule["end"]):
            return rule["tm"]
    return None


class MenuItem(ButtonBehavior, MDLabel):
    """自定义下拉菜单项：可点击 + 支持黑体"""
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.font_name = FONT_PATH
        self.theme_text_color = "Primary"
        self.adaptive_height = True
        self.padding = [dp(16), dp(14)]
        self.halign = "left"


class NoAnimDropdownMenu(MDDropdownMenu):
    """
    完全接管打开逻辑：
    1. 强制从按钮下方弹出（不自动改为顶部）
    2. 跳过默认缩放/位移动画，避免 scale 异常导致看不到选项
    """

    def adjust_position(self):
        return "bottom"

    def open(self):
        self.set_menu_properties()
        Window.add_widget(self)
        if self.width <= dp(100):
            self.width = dp(240)
        self.height = self.target_height
        self._scale_x = 1
        self._scale_y = 1
        self.set_menu_pos()

    def on_open(self):
        pass


class SelectorButton(ButtonBehavior, MDBoxLayout):
    """
    自定义选择按钮：带完全封闭的圆角矩形边框，
    点击后弹出下拉菜单。不使用 MDTextField，从而避免 rectangle 模式顶部缺口。
    """
    def __init__(self, text="", options=None, callback=None, **kwargs):
        super().__init__(**kwargs)
        self.orientation = "horizontal"
        self.size_hint = (1, None)
        self.height = dp(46)
        self.padding = [dp(12), 0]
        self.options = options or []
        self.callback = callback

        # 绘制完全封闭的圆角边框
        with self.canvas.before:
            Color(0.6, 0.6, 0.6, 1)
            self._border = Line(
                rounded_rectangle=(self.x, self.y, self.width, self.height, dp(8)),
                width=1.2
            )
        self.bind(pos=self._update_border)
        self.bind(size=self._update_border)

        self.label = MDLabel(
            text=text,
            halign="left",
            valign="center",
            theme_text_color="Primary",
            font_name=FONT_PATH,
            font_size="14sp",
        )
        self.add_widget(self.label)

    def _update_border(self, *args):
        self._border.rounded_rectangle = (self.x, self.y, self.width, self.height, dp(8))

    def on_touch_down(self, touch):
        if self.collide_point(*touch.pos):
            self._open_menu()
            return True
        return super().on_touch_down(touch)

    def _open_menu(self):
        menu_items = [
            {
                "viewclass": "MenuItem",
                "text": opt,
                "on_release": lambda x=opt: self._select(x),
            } for opt in self.options
        ]
        self.menu = NoAnimDropdownMenu(
            caller=self,
            items=menu_items,
            position="bottom",
            width_mult=2,
        )
        self.menu.max_height = dp(280)
        self.menu.open()

    def _select(self, text):
        self.label.text = text
        if self.callback:
            self.callback(text)
        # 延迟 dismiss，避免在触摸事件处理中直接销毁菜单导致闪退
        Clock.schedule_once(lambda dt: self.menu.dismiss(), 0)


class MDTimePicker(MDBoxLayout):
    """
    Material 风格的时间滚轮选择器：
    由两个 SelectorButton（小时 / 分钟）和中间的冒号组成。
    边框完全封闭，无 hint_text。
    """
    def __init__(self, initial_hour="09", initial_minute="00", **kwargs):
        super().__init__(**kwargs)
        self.orientation = "horizontal"
        self.spacing = dp(8)
        self.size_hint = (1, None)
        self.height = dp(46)

        self.hour_field = SelectorButton(
            text=initial_hour,
            options=[f"{i:02d}" for i in range(24)],
        )
        self.minute_field = SelectorButton(
            text=initial_minute,
            options=[f"{i:02d}" for i in range(60)],
        )
        colon = MDLabel(
            text=":",
            halign="center",
            valign="center",
            size_hint=(0.16, 1),
            font_size="22sp",
            bold=True,
            theme_text_color="Secondary",
        )
        self.add_widget(self.hour_field)
        self.add_widget(colon)
        self.add_widget(self.minute_field)

    def get_time_minutes(self) -> int:
        return int(self.hour_field.label.text) * 60 + int(self.minute_field.label.text)

    def get_time_str(self) -> str:
        return f"{self.hour_field.label.text}:{self.minute_field.label.text}"


# Material Icons 字体路径（已复制到项目目录，避免加载失败导致方框）
MDI_FONT_PATH = "materialdesignicons-webfont.ttf"


class MainScreen(MDScreen):
    def __init__(self, app, **kwargs):
        super().__init__(**kwargs)
        self.app = app
        self._build_ui()

    def _build_ui(self):
        # 根布局：垂直排列
        root_box = MDBoxLayout(orientation="vertical")

        # ----------------------
        # 顶部 AppBar 区域（用 FloatLayout 让按钮浮在标题右侧）
        # ----------------------
        toolbar_box = FloatLayout(size_hint=(1, None), height=dp(56))

        toolbar = MDTopAppBar(
            title="飞行机组执勤时间计算器",
            elevation=4,
            pos_hint={"top": 1},
            size_hint=(1, None),
            height=dp(56),
        )
        # 修正标题字体为黑体
        toolbar.ids.label_title.font_name = FONT_PATH

        # 使用 MDIconButton 作为主题切换图标（太阳/月亮）
        # 已在 App.build() 中将 fonts_path 指向项目目录，确保字体能正确加载
        self.theme_btn = MDIconButton(
            icon="weather-night",  # 默认亮色模式下显示月亮（点击切暗色）
            theme_text_color="Custom",
            text_color=(1, 1, 1, 1),
            pos_hint={"right": 0.98, "center_y": 0.5},
            size_hint=(None, None),
            size=(dp(48), dp(48)),
        )
        self.theme_btn.bind(on_release=lambda x: self.app.toggle_theme())

        toolbar_box.add_widget(toolbar)
        toolbar_box.add_widget(self.theme_btn)
        root_box.add_widget(toolbar_box)

        # ----------------------
        # 可滚动内容区
        # ----------------------
        scroll = MDScrollView()
        content = MDBoxLayout(
            orientation="vertical",
            adaptive_height=True,
            padding=dp(16),
            spacing=dp(16),
        )

        # ========== 飞行信息卡片 ==========
        flight_card = MDCard(
            radius=[dp(16)],
            elevation=dp(4),
            padding=dp(16),
            size_hint=(1, None),
        )
        flight_box = MDBoxLayout(
            orientation="vertical",
            adaptive_height=True,
            spacing=dp(12),
        )

        # 两列网格布局（左对齐）
        grid = MDGridLayout(
            cols=2,
            spacing=[dp(12), dp(12)],
            adaptive_height=True,
            size_hint=(1, None),
            row_default_height=dp(46),
            row_force_default=True,
        )

        # 1) 机组配置
        grid.add_widget(MDLabel(
            text="机组配置",
            font_name=FONT_PATH,
            font_size="14sp",
            theme_text_color="Secondary",
            halign="left",
            valign="center",
            size_hint=(1, 1),
        ))
        self.crew_field = SelectorButton(
            text="非扩编组",
            options=["非扩编组", "扩编组（3人）", "扩编组（4人）"],
            callback=self.on_crew_change,
        )
        grid.add_widget(self.crew_field)

        # 2) 起飞时间（滚轮选择器）
        grid.add_widget(MDLabel(
            text="起飞时间",
            font_name=FONT_PATH,
            font_size="14sp",
            theme_text_color="Secondary",
            halign="left",
            valign="center",
            size_hint=(1, 1),
        ))
        self.takeoff_picker = MDTimePicker(initial_hour="09", initial_minute="00")
        grid.add_widget(self.takeoff_picker)

        # 3) 航段数
        grid.add_widget(MDLabel(
            text="航段数",
            font_name=FONT_PATH,
            font_size="14sp",
            theme_text_color="Secondary",
            halign="left",
            valign="center",
            size_hint=(1, 1),
        ))
        self.leg_field = SelectorButton(
            text="1-4段",
            options=["1-4段", "5段", "6段", "7段"],
        )
        grid.add_widget(self.leg_field)

        # 4) 休息设施
        grid.add_widget(MDLabel(
            text="休息设施",
            font_name=FONT_PATH,
            font_size="14sp",
            theme_text_color="Secondary",
            halign="left",
            valign="center",
            size_hint=(1, 1),
        ))
        self.rest_field = SelectorButton(
            text="1级休息设施",
            options=["1级休息设施", "2级休息设施", "3级休息设施"],
        )
        self.rest_field.opacity = 0
        self.rest_field.disabled = True
        grid.add_widget(self.rest_field)

        # 5) 休息间隔（滚轮选择器）
        grid.add_widget(MDLabel(
            text="休息间隔",
            font_name=FONT_PATH,
            font_size="14sp",
            theme_text_color="Secondary",
            halign="left",
            valign="center",
            size_hint=(1, 1),
        ))
        self.rest_picker = MDTimePicker(initial_hour="00", initial_minute="00")
        grid.add_widget(self.rest_picker)

        flight_box.add_widget(grid)
        flight_card.add_widget(flight_box)
        flight_box.bind(minimum_height=lambda inst, val: setattr(flight_card, "height", val + dp(32)))
        content.add_widget(flight_card)

        # ========== 计算按钮 ==========
        calc_btn = MDRaisedButton(
            text="开始计算",
            font_name=FONT_PATH,
            font_size="16sp",
            size_hint=(None, None),
            width=dp(200),
            height=dp(46),
            pos_hint={"center_x": 0.5},
        )
        calc_btn.bind(on_release=self.calculate)
        content.add_widget(calc_btn)

        # ========== 计算结果卡片 ==========
        result_card = MDCard(
            radius=[dp(16)],
            elevation=dp(4),
            padding=dp(16),
            size_hint=(1, None),
        )
        result_box = MDBoxLayout(
            orientation="vertical",
            adaptive_height=True,
            spacing=dp(12),
        )
        self.result_label = MDLabel(
            text="请输入信息后点击「开始计算」",
            font_name=FONT_PATH,
            markup=True,
            font_size="18sp",
            line_height=1.6,
            theme_text_color="Primary",
            adaptive_height=True,
            halign="left",
        )
        result_box.add_widget(self.result_label)
        result_card.add_widget(result_box)
        result_box.bind(minimum_height=lambda inst, val: setattr(result_card, "height", val + dp(32)))
        content.add_widget(result_card)

        scroll.add_widget(content)
        root_box.add_widget(scroll)
        self.add_widget(root_box)

        # 初始化控件可见性
        Clock.schedule_once(lambda dt: self.on_crew_change("非扩编组"), 0)

    def on_crew_change(self, text):
        """切换机组配置时，动态显示/隐藏航段数或休息设施"""
        if text == "非扩编组":
            self.leg_field.opacity = 1
            self.leg_field.disabled = False
            self.rest_field.opacity = 0
            self.rest_field.disabled = True
        else:
            self.leg_field.opacity = 0
            self.leg_field.disabled = True
            self.rest_field.opacity = 1
            self.rest_field.disabled = False

    def calculate(self, instance):
        """核心计算逻辑"""
        # 解析时间
        takeoff_min = self.takeoff_picker.get_time_minutes()
        takeoff_str = self.takeoff_picker.get_time_str()
        rest_interval_min = self.rest_picker.get_time_minutes()

        # 查表参数
        takeoff_hhmm = f"{takeoff_min // 60:02d}{takeoff_min % 60:02d}"
        people_key = PEOPLE_MAP[self.crew_field.label.text]

        # 查最大飞行时间
        max_flt = lookup_value("flt", people_key, takeoff_hhmm)
        if max_flt is None:
            self.result_label.text = "[color=#ff0000]错误：未找到对应的飞行时间规则[/color]"
            return

        # 查最大执勤时间
        if people_key == "非扩编":
            leg = LEG_MAP[self.leg_field.label.text]
            max_duty = lookup_value("duty", people_key, takeoff_hhmm, leg=leg)
        else:
            max_duty = lookup_value("duty", people_key, takeoff_hhmm, rest=self.rest_field.label.text)

        if max_duty is None:
            self.result_label.text = "[color=#ff0000]错误：未找到对应的执勤时间规则[/color]"
            return

        # 临界时间计算
        flt_critical = takeoff_min + max_flt * 60
        duty_critical = takeoff_min + max_duty * 60
        duty_critical_with_rest = duty_critical + rest_interval_min

        # 组装结果文本
        lines = []
        lines.append(f"[b]机组配置：[/b]{self.crew_field.label.text}")
        lines.append(f"[b]起飞时间：[/b]{takeoff_str}")
        if people_key == "非扩编":
            lines.append(f"[b]航段数：[/b]{self.leg_field.label.text}")
        else:
            lines.append(f"[b]休息设施：[/b]{self.rest_field.label.text}")
        if rest_interval_min > 0:
            lines.append(f"[b]休息间隔：[/b]{self.rest_picker.get_time_str()}")
        lines.append("")
        lines.append(f"[size=22sp][color=#1a4a8c][b]最大飞行时间：{max_flt} 小时[/b][/color][/size]")
        lines.append(f"[size=22sp][color=#1a4a8c][b]最大执勤时间：{max_duty} 小时[/b][/color][/size]")
        lines.append("")
        lines.append(f"[b]执勤超时临界时间：[/b][color=#f2611d]{minutes_to_hhmm(duty_critical)}[/color]")
        if rest_interval_min > 0:
            lines.append(f"[b]含休息间隔的执勤临界时间：[/b][color=#f2611d]{minutes_to_hhmm(duty_critical_with_rest)}[/color]")
        lines.append("")
        lines.append("[size=12sp][color=#737373]说明：临界时间 = 起飞时间 + 对应时限；若填写了休息间隔，则额外累加至执勤临界时间中。[/color][/size]")

        self.result_label.text = "\n".join(lines)


class FlightTimeApp(MDApp):
    """
    KivyMD 应用主类：
    - 主色调设为 Blue 900（深蓝色）
    - 支持亮/暗主题切换
    - 全局字体替换为黑体，彻底避免乱码
    """
    def build(self):
        self.theme_cls.primary_palette = "Blue"
        self.theme_cls.primary_hue = "900"
        self.theme_cls.theme_style = "Light"
        # 强制 KivyMD 从项目目录加载 Material Icons 字体，避免方框
        self.theme_cls.fonts_path = os.path.dirname(os.path.abspath(__file__)) + os.sep
        # 将 KivyMD 所有主题字体样式替换为黑体
        for key in list(self.theme_cls.font_styles.keys()):
            style = self.theme_cls.font_styles[key]
            self.theme_cls.font_styles[key] = [FONT_PATH, style[1], style[2], style[3]]
        return MainScreen(app=self)

    def toggle_theme(self):
        """点击按钮切换主题，并更新图标"""
        screen = self.root
        if self.theme_cls.theme_style == "Light":
            self.theme_cls.theme_style = "Dark"
            screen.theme_btn.icon = "weather-sunny"  # 暗色模式下显示太阳（点击切亮色）
        else:
            self.theme_cls.theme_style = "Light"
            screen.theme_btn.icon = "weather-night"  # 亮色模式下显示月亮（点击切暗色）


if __name__ == "__main__":
    FlightTimeApp().run()
