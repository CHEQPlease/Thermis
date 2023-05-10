import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'dart:async';

import 'package:thermis/thermis.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String demotext = "Demo Text";
  Uint8List? imageBytes;

  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {

  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Column(
          children: [
            ElevatedButton(
              onPressed: () async {
                String receiptDTOJSON = await DefaultAssetBundle.of(context).loadString('assets/data.json');
                imageBytes = await Thermis.previewReceipt(receiptDTOJSON);
                setState(() {});
              },
              child: const Text("Test Print"),
            ),

              if(imageBytes != null)...{
                Expanded(
                  child: Center(child: Image.memory(imageBytes!)),
                )
              }
          ],
        ),
      ),
    );
  }
}
