import hilog from "@ohos.hilog";
import window from "@ohos.window";
import UIAbility from "@ohos.app.ability.UIAbility";

export default class EntryAbility extends UIAbility {

  onCreate(want, launchParam) {
    hilog.info(0x0000, "EntryAbility", "onCreate");
  }

  onDestroy() {
    hilog.info(0x0000, "EntryAbility", "onDestroy");
  }

  onWindowStageCreate(windowStage: window.WindowStage) {
    hilog.info(0x0000, "EntryAbility", "onWindowStageCreate");
    windowStage.loadContent("pages/Index", (err, data) => {
    });
  }

  onWindowStageDestroy() {
    hilog.info(0x0000, "EntryAbility", "onWindowStageDestroy");
  }

  onForeground() {
    hilog.info(0x0000, "EntryAbility", "onForeground");
  }

  onBackground() {
    hilog.info(0x0000, "EntryAbility", "onBackground");
  }

};
