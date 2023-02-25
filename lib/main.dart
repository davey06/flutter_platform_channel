import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        // This is the theme of your application.
        //
        // Try running your application with "flutter run". You'll see the
        // application has a blue toolbar. Then, without quitting the app, try
        // changing the primarySwatch below to Colors.green and then invoke
        // "hot reload" (press "r" in the console where you ran "flutter run",
        // or simply save your changes to "hot reload" in a Flutter IDE).
        // Notice that the counter didn't reset back to zero; the application
        // is not restarted.
        primarySwatch: Colors.blue,
      ),
      home: const MyHomePage(title: 'Flutter Demo Home Page'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key, required this.title});

  // This widget is the home page of your application. It is stateful, meaning
  // that it has a State object (defined below) that contains fields that affect
  // how it looks.

  // This class is the configuration for the state. It holds the values (in this
  // case the title) provided by the parent (in this case the App widget) and
  // used by the build method of the State. Fields in a Widget subclass are
  // always marked "final".

  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  static const channelName = 'example.flutter/platform_channel';
  static const platform = MethodChannel(channelName);
  // Get battery level.
  String _nativeMessage = '';

  Future<void> _getBatteryLevel() async {
    try {
      final int result = await platform.invokeMethod('getBatteryLevel');
      _nativeMessage = 'Battery level at $result % .';
    } on PlatformException catch (e) {
      _nativeMessage = "Failed to get battery level: '${e.message}'.";
    }
    setState(() {});
  }

  Future<void> doNativeSuff() async {
    try {
      final String result = await platform.invokeMethod('changeLife');
      _nativeMessage = result;
    } on PlatformException catch (e) {
      _nativeMessage = "Sadly I can not change your life: ${e.message}.";
    }
    setState(() {});
  }

  Future<void> getDeviceInfo() async {
    try {
      final result = await platform.invokeMethod('getDeviceInfo');
      print(result.runtimeType);
      _nativeMessage = result.toString();
    } on PlatformException catch (e) {
      _nativeMessage = "Failed to get device Info: ${e.message}.";
    }
    setState(() {});
  }

  Future<void> getCPUUsage() async {
    try {
      final result = await platform.invokeMethod('getCPUUsage');
      print(result);
      print(result.runtimeType);
      _nativeMessage = result.toString();
    } on PlatformException catch (e) {
      _nativeMessage = "Failed to get CPU Info: ${e.message}.";
    }
    setState(() {});
  }

  Future<void> getCPU2() async {
    try {
      final result = await platform.invokeMethod('getCPU2');
      print(result);
      print(result.runtimeType);
      _nativeMessage = result.toString();
    } on PlatformException catch (e) {
      _nativeMessage = "Failed to get CPU Info: ${e.message}.";
    }
    setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    return Material(
      child: Center(
        child: Column(
          children: [
            ElevatedButton(
              onPressed: _getBatteryLevel,
              child: const Text('Get Battery Level'),
            ),
            ElevatedButton(
              onPressed: getDeviceInfo,
              child: const Text('Get Device Info'),
            ),
            ElevatedButton(
              onPressed: doNativeSuff,
              child: const Text('Do Native Stuff'),
            ),
            ElevatedButton(
              onPressed: getCPUUsage,
              child: const Text('Get CPU USAGE 1'),
            ),
            ElevatedButton(
              onPressed: getCPU2,
              child: const Text('Get CPU USAGE 2'),
            ),
            const Spacer(),
            Padding(
              padding: const EdgeInsets.all(20),
              child: Text(_nativeMessage),
            ),
          ],
        ),
      ),
    );
  }
}
