package com.acgist.taoyao;

import com.acgist.taoyao.slice.MainAbilitySlice;
import com.acgist.taoyao.slice.SettingAbilitySlice;
import ohos.aafwk.ability.Ability;
import ohos.aafwk.content.Intent;

public class MainAbility extends Ability {
    @Override
    public void onStart(Intent intent) {
        super.onStart(intent);
        super.setMainRoute(MainAbilitySlice.class.getName());
        super.addActionRoute("main", MainAbilitySlice.class.getName());
        super.addActionRoute("setting", SettingAbilitySlice.class.getName());
    }
}
