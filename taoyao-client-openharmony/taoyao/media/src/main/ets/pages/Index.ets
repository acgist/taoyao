import { signal } from "../taoyao/TaoyaoSignal";

@Entry
@Component
struct Index {

  build() {
    Row() {
      Column() {
        Button("加载系统")
          .fontSize(20)
          .fontWeight(FontWeight.Bold)
          .onClick(() => {
            signal.init();
          });
      }
      .width("50%");
      Column() {
        Button("连接信令")
          .fontSize(20)
          .fontWeight(FontWeight.Bold)
          .onClick(() => {
            signal.connect();
          });
      }
      .width("50%");
      Column() {
        Button("断开信令")
          .fontSize(20)
          .fontWeight(FontWeight.Bold)
          .onClick(() => {
            signal.close();
          });
      }
      .width("50%");
      Column() {
        Button("卸载系统")
          .fontSize(20)
          .fontWeight(FontWeight.Bold)
          .onClick(() => {
            signal.shutdown();
          });
      }
      .width("50%");
    }
    .height("100%");
  }

}
