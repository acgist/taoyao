package com.acgist.taoyao;

import ohos.aafwk.ability.AbilityPackage;

public class TaoyaoApplication extends AbilityPackage {

    static {
        System.loadLibrary("taoyao");
    }

    @Override
    public void onInitialize() {
        super.onInitialize();
    }

}
