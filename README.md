# BSpring
自己尝试写简易版spring,练习记录

主要流程：
//加载配置文件
doLoadConf(config.getInitParameter("contextConfigLocation"));
// 扫描类
doScanPackage(properties.getProperty("sanPackage"));
// 初始化实例
doInstance();
// 注入
doResourced();
// 构造handlerMap
initHandlerMap();
// 匹配uri 定位方法  回调
