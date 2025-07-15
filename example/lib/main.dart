import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'dart:async';

import 'package:thermis/printer_config.dart';
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
  bool isInitialized = false; // State variable to track initialization
  String selectedPrinterType = 'GENERIC'; // State variable for selected printer type

  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    // Initialize the Thermis plugin with a PrinterConfig
    PrinterConfig config = PrinterConfig(
      printerType: selectedPrinterType, // Use selected printer type
      printerMAC: '00:11:22:33:44:55', // Correct parameter name
    );
    isInitialized = await Thermis.init(config) ?? false;
    setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      theme: ThemeData(
        primarySwatch: Colors.blue,
        visualDensity: VisualDensity.adaptivePlatformDensity,
      ),
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Thermis Plugin Example'),
        ),
        body: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  const Text('Initialization Status: '),
                  Icon(
                    Icons.circle,
                    color: isInitialized ? Colors.green : Colors.red,
                    size: 12,
                  ),
                ],
              ),
              const SizedBox(height: 20),
              Row(
                children: [
                  const Text('Select Printer Type: '),
                  DropdownButton<String>(
                    value: selectedPrinterType,
                    items: <String>['GENERIC', 'STAR']
                        .map<DropdownMenuItem<String>>((String value) {
                      return DropdownMenuItem<String>(
                        value: value,
                        child: Text(value),
                      );
                    }).toList(),
                    onChanged: (String? newValue) {
                      setState(() {
                        selectedPrinterType = newValue!;
                        initPlatformState(); // Re-initialize with new printer type
                      });
                    },
                  ),
                ],
              ),
              const SizedBox(height: 20),
              ElevatedButton(
                onPressed: () async {
                  await initPlatformState();
                },
                child: const Text('Initialize'),
              ),
              const SizedBox(height: 20),
              Card(
                elevation: 4,
                child: Padding(
                  padding: const EdgeInsets.all(16.0),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text(
                        'Receipt Operations',
                        style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                      ),
                      const SizedBox(height: 10),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                        children: [
                          Flexible(
                            child: ElevatedButton.icon(
                              onPressed: () async {
                                String customerJSON = await DefaultAssetBundle.of(context).loadString('assets/customer.json');
                                Thermis.printCHEQReceipt(customerJSON);
                              },
                              icon: const Icon(Icons.print),
                              label: const Text('Print Receipt'),
                            ),
                          ),
                          const SizedBox(width: 10), // Add spacing between buttons
                          Flexible(
                            child: ElevatedButton.icon(
                              onPressed: () async {
                                String customerJSON = await DefaultAssetBundle.of(context).loadString('assets/customer.json');
                                imageByets = await Thermis.previewReceipt(customerJSON);
                                setState(() {});
                              },
                              icon: const Icon(Icons.preview),
                              label: const Text('Preview Receipt'),
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 20),
              if (imageByets != null)
                Expanded(
                  child: Container(
                    color: Colors.grey[200],
                    child: Center(
                      child: Image.memory(imageByets!),
                    ),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}
