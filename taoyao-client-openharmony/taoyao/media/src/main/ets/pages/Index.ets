import { taoyaoSignal } from "../taoyao/TaoyaoSignal";

import { setting } from "../taoyao/Setting";

@Entry
@Component
struct Index {

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
