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

  Uint8List? imageByets;

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
            Row(
              children: [
                ElevatedButton(
                  onPressed: () async {
                    // String customerJSON = await DefaultAssetBundle.of(context).loadString('assets/customer.json');
                    // String merchantJSON = await DefaultAssetBundle.of(context).loadString('assets/merchant.json');
                     String kitchen = await DefaultAssetBundle.of(context).loadString('assets/kitchen.json');
                    String serverTips = await DefaultAssetBundle.of(context).loadString('assets/server_tips.json');
                    Thermis.printCHEQReceipt(serverTips);
                    Thermis.printCHEQReceipt(kitchen);
                  },
                  child: const Text('Print Receipt'),
                ),
                ElevatedButton(
                  onPressed: () async {
                    String customerJSON = await DefaultAssetBundle.of(context).loadString('assets/customer_total_split.json');
                    imageByets = await Thermis.previewReceipt(customerJSON);
                    setState(()  {

                    });

                  },
                  child: const Text('Preview Receipt'),
                ),
              ],
            ),
            if(imageByets != null)
              Expanded(child: Container(
                  color: Colors.redAccent,
                  margin: const EdgeInsets.all(0),
                  child: Image.memory(imageByets!))),
          ],
        ),
      ),
    );
  }
}
