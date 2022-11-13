/** 桃夭WebRTC终端应用功能 */
// 样式操作
function classSwitch(e, className) {
	if(e.className.indexOf(className) >= 0) {
		e.className = e.className.replace(className, '').replace(/(^\s)|(\s$)/g, "");
	} else {
		e.className = e.className + ' ' + className;
	}
}
