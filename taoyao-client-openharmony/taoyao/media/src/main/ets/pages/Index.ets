import { taoyaoSignal } from "../taoyao/TaoyaoSignal";

import fs from '@ohos.file.fs';
import { BusinessError } from "@ohos.base";
import common from "@ohos.app.ability.common";
import abilityAccessCtrl, { Permissions } from "@ohos.abilityAccessCtrl";

import { setting } from "../taoyao/Setting";

const permissions: Array<Permissions> = [
  "ohos.permission.CAMERA",
  "ohos.permission.INTERNET",
  "ohos.permission.MICROPHONE",
  "ohos.permission.READ_MEDIA",
  "ohos.permission.WRITE_MEDIA",
  "ohos.permission.CAPTURE_SCREEN",
  "ohos.permission.CAPTURE_VOICE_DOWNLINK_AUDIO",
];

function reqPermissionsFromUser(permissions: Array<Permissions>, context: common.UIAbilityContext) {
  abilityAccessCtrl.createAtManager().requestPermissionsFromUser(context, permissions).then((data) => {
    // 拷贝证书
    const caPath = context.filesDir + setting.caPath;
    const caFile = fs.createRandomAccessFileSync(caPath, fs.OpenMode.CREATE | fs.OpenMode.READ_WRITE);
    const caText = context.resourceManager.getRawFileContentSync("cacert.pem").buffer;
    caFile.writeSync(caText);
    caFile.close();
    // 自动连接信令
    if(setting.initOnLoad) {
      taoyaoSignal.connect();
    }
  }).catch((err: BusinessError) => {
  });
}

@Entry
@Component
struct Index {

  aboutToAppear() {
    const context = getContext(this) as common.UIAbilityContext;
    reqPermissionsFromUser(permissions, context);
  }

  build() {
    Column() {
      Column() {
        Button("连接信令")
          .fontSize(20)
          .fontWeight(FontWeight.Bold)
          .onClick(() => {
            taoyaoSignal.connect();
          });
      }
      .width("100%")
      .height("20%");
      Column() {
        Button("断开信令")
          .fontSize(20)
          .fontWeight(FontWeight.Bold)
          .onClick(() => {
            taoyaoSignal.close();
          });
      }
      .width("100%")
      .height("20%");
      Column() {
        Button("加载系统")
          .fontSize(20)
          .fontWeight(FontWeight.Bold)
          .onClick(() => {
            taoyaoSignal.init();
          });
      }
      .width("100%")
      .height("20%");
      Column() {
        Button("卸载系统")
          .fontSize(20)
          .fontWeight(FontWeight.Bold)
          .onClick(() => {
            taoyaoSignal.shutdown();
          });
      }
      .width("100%")
      .height("20%");
      Column() {
        Text(
          `
          CA证书：${getContext(this).resourceDir + setting.caPath}
          信令地址：${setting.signalAddress}
          `
        )
        .fontSize(20)
      }
      .width("100%")
      .height("20%");
    }
    .height("100%");
  }

}
