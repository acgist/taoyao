package com.acgist.taoyao.slice;

import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import ohos.agp.colors.RgbColor;
import ohos.agp.components.AttrHelper;
import ohos.agp.components.Button;
import ohos.agp.components.ComponentContainer.LayoutConfig;
import ohos.agp.components.DirectionalLayout;
import ohos.agp.components.Text;
import ohos.agp.components.element.ShapeElement;
import ohos.agp.utils.Color;

public class SettingAbilitySlice extends AbilitySlice {
    // Load the 'native-lib' library on application startup.

    private final DirectionalLayout layout = new DirectionalLayout(this);

    @Override
    public void onStart(Intent intent) {
        super.onStart(intent);
        LayoutConfig config = new LayoutConfig(LayoutConfig.MATCH_PARENT, LayoutConfig.MATCH_PARENT);
        layout.setLayoutConfig(config);
        layout.setAlignment(17);
        ShapeElement element = new ShapeElement();
        element.setShape(ShapeElement.RECTANGLE);
        element.setRgbColor(new RgbColor(255, 255, 255));
        layout.setBackground(element);
        Text text = new Text(this);
        text.setText("1234");
        text.setTextColor(Color.BLACK);
        text.setTextSize(AttrHelper.vp2px(26, this));
        layout.addComponent(text);
        Button button = new Button(this);
        button.setText("保存");
        button.setTextSize(AttrHelper.vp2px(26, this));
        button.setClickedListener(listener -> super.present(new MainAbilitySlice(), new Intent()));
        layout.addComponent(button);
        super.setUIContent(layout);
    }

    @Override
    public void onActive() {
        super.onActive();
    }

    @Override
    public void onForeground(Intent intent) {
        super.onForeground(intent);
        // 打开预览
    }

    @Override
    protected void onBackground() {
        super.onBackground();
        // 掐断预览
    }

}
