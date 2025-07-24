import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:thermis/printer_config.dart';
import 'package:thermis/thermis.dart';
import 'package:thermis/device.dart';
import 'dart:async';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      theme: ThemeData(
        brightness: Brightness.dark,
        scaffoldBackgroundColor: const Color(0xFF1A1B1E),
        cardColor: const Color(0xFF2A2B2E),
        appBarTheme: const AppBarTheme(
          backgroundColor: Color(0xFF1A1B1E),
          elevation: 0,
        ),
        elevatedButtonTheme: ElevatedButtonThemeData(
          style: ElevatedButton.styleFrom(
            backgroundColor: const Color(0xFF2962FF),
            foregroundColor: Colors.white,
            minimumSize: const Size.fromHeight(56),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(12),
            ),
          ),
        ),
      ),
      home: const HomeScreen(),
    );
  }
}

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  String selectedPrinter = '-';
  StreamSubscription<Device>? _discoverySubscription;
  List<Device> discoveredPrinters = [];
  bool isDiscovering = false;
  StateSetter? _dialogSetState;

  @override
  void dispose() {
    _discoverySubscription?.cancel();
    super.dispose();
  }

  void _startDiscovery() {
    setState(() {
      isDiscovering = true;
      discoveredPrinters.clear();
    });
    
    _showDiscoveryDialog();

    _discoverySubscription?.cancel();
    _discoverySubscription = Thermis.discoverPrinters().listen(
      (device) {
        if (_dialogSetState != null) {
          _dialogSetState!(() {
            discoveredPrinters.add(device);
          });
        }
      },
      onError: (error) {
        print('Error during discovery: $error');
        if (_dialogSetState != null) {
          _dialogSetState!(() {
            isDiscovering = false;
          });
        }
      },
      onDone: () {
        print('Discovery finished');
        if (_dialogSetState != null) {
          _dialogSetState!(() {
            isDiscovering = false;
          });
        }
      },
    );
  }

  void _stopDiscovery() {
    _discoverySubscription?.cancel();
    Thermis.stopDiscovery();
    if (_dialogSetState != null) {
      _dialogSetState!(() {
        isDiscovering = false;
      });
    }
  }

  Future<void> _showPreviewDialog(BuildContext context, Uint8List imageBytes) {
    return showDialog(
      context: context,
      builder: (BuildContext context) {
        return Dialog(
          backgroundColor: const Color(0xFF2A2B2E),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text(
                      'Receipt Preview',
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w500,
                        color: Colors.white,
                      ),
                    ),
                    IconButton(
                      icon: const Icon(Icons.close, color: Colors.white, size: 20),
                      padding: EdgeInsets.zero,
                      constraints: const BoxConstraints(),
                      onPressed: () => Navigator.of(context).pop(),
                    ),
                  ],
                ),
              ),
              Container(
                constraints: const BoxConstraints(
                  maxHeight: 500,
                ),
                child: SingleChildScrollView(
                  child: Image.memory(
                    imageBytes,
                    fit: BoxFit.contain,
                  ),
                ),
              ),
              const SizedBox(height: 16),
            ],
          ),
        );
      },
    );
  }

  Future<void> _showDiscoveryDialog() async {
    await showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (BuildContext context) {
        return StatefulBuilder(
          builder: (BuildContext context, StateSetter setDialogState) {
            _dialogSetState = setDialogState;
            return Dialog(
              backgroundColor: const Color(0xFF2A2B2E),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(16),
              ),
              child: ConstrainedBox(
                constraints: const BoxConstraints(
                  maxWidth: 400,
                  maxHeight: 600,
                ),
                child: Padding(
                  padding: const EdgeInsets.all(16.0),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          const Expanded(
                            child: Text(
                              'Discovered Printers',
                              style: TextStyle(
                                fontSize: 20,
                                fontWeight: FontWeight.bold,
                                color: Colors.white,
                              ),
                              overflow: TextOverflow.ellipsis,
                            ),
                          ),
                          Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              if (isDiscovering)
                                Container(
                                  width: 24,
                                  height: 24,
                                  margin: const EdgeInsets.only(right: 12),
                                  child: const CircularProgressIndicator(
                                    strokeWidth: 2,
                                    valueColor: AlwaysStoppedAnimation<Color>(Color(0xFF5C4FE1)),
                                  ),
                                ),
                              IconButton(
                                icon: const Icon(Icons.refresh, color: Color(0xFF5C4FE1)),
                                onPressed: isDiscovering
                                    ? null
                                    : () {
                                        discoveredPrinters.clear();
                                        _startDiscovery();
                                      },
                                padding: const EdgeInsets.all(8),
                                constraints: const BoxConstraints(
                                  minWidth: 36,
                                  minHeight: 36,
                                ),
                              ),
                              IconButton(
                                icon: const Icon(Icons.close, color: Colors.white54),
                                onPressed: () {
                                  _stopDiscovery();
                                  Navigator.of(context).pop();
                                },
                                padding: const EdgeInsets.all(8),
                                constraints: const BoxConstraints(
                                  minWidth: 36,
                                  minHeight: 36,
                                ),
                              ),
                            ],
                          ),
                        ],
                      ),
                      const SizedBox(height: 8),
                      Text(
                        isDiscovering
                            ? 'Searching for available printers...'
                            : discoveredPrinters.isEmpty
                                ? 'No printers found. Try refreshing.'
                                : '${discoveredPrinters.length} printer(s) found',
                        style: TextStyle(
                          color: Colors.white.withOpacity(0.7),
                          fontSize: 14,
                        ),
                      ),
                      const SizedBox(height: 16),
                      Flexible(
                        child: discoveredPrinters.isEmpty && !isDiscovering
                            ? Center(
                                child: Column(
                                  mainAxisSize: MainAxisSize.min,
                                  children: [
                                    Icon(
                                      Icons.print_disabled,
                                      size: 48,
                                      color: Colors.white.withOpacity(0.3),
                                    ),
                                    const SizedBox(height: 16),
                                    Text(
                                      'No printers found',
                                      style: TextStyle(
                                        color: Colors.white.withOpacity(0.7),
                                        fontSize: 16,
                                      ),
                                    ),
                                    const SizedBox(height: 8),
                                    Text(
                                      'Make sure your printer is powered on\nand connected to the network',
                                      textAlign: TextAlign.center,
                                      style: TextStyle(
                                        color: Colors.white.withOpacity(0.5),
                                        fontSize: 14,
                                      ),
                                    ),
                                  ],
                                ),
                              )
                            : ListView.separated(
                                padding: const EdgeInsets.symmetric(vertical: 8),
                                itemCount: discoveredPrinters.length,
                                separatorBuilder: (context, index) => const Divider(
                                  color: Colors.white10,
                                  height: 1,
                                ),
                                itemBuilder: (context, index) {
                                  final printer = discoveredPrinters[index];
                                  final bool isSelected = selectedPrinter == printer.mac;
                                  
                                  return Material(
                                    color: Colors.transparent,
                                    child: InkWell(
                                      onTap: () {
                                        setState(() {
                                          selectedPrinter = printer.mac ?? '-';
                                        });
                                        _stopDiscovery();
                                        Navigator.of(context).pop();
                                      },
                                      borderRadius: BorderRadius.circular(8),
                                      child: Container(
                                        padding: const EdgeInsets.symmetric(
                                          horizontal: 0,
                                          vertical: 12,
                                        ),
                                        decoration: BoxDecoration(
                                          borderRadius: BorderRadius.circular(8),
                                          border: Border.all(
                                            color: isSelected
                                                ? const Color(0xFF5C4FE1)
                                                : Colors.transparent,
                                            width: 2,
                                          ),
                                        ),
                                        child: Row(
                                          mainAxisAlignment: MainAxisAlignment.start,
                                          children: [
                                            Container(
                                              width: 40,
                                              height: 40,
                                              decoration: BoxDecoration(
                                                color: const Color(0xFF5C4FE1).withOpacity(0.1),
                                                borderRadius: BorderRadius.circular(20),
                                              ),
                                              child: const Icon(
                                                Icons.print,
                                                color: Color(0xFF5C4FE1),
                                                size: 20,
                                              ),
                                            ),
                                            const SizedBox(width: 16),
                                            Expanded(
                                              child: Column(
                                                crossAxisAlignment: CrossAxisAlignment.start,
                                                children: [
                                                  Text(
                                                    printer.deviceName,
                                                    style: const TextStyle(
                                                      color: Colors.white,
                                                      fontSize: 16,
                                                      fontWeight: FontWeight.w500,
                                                    ),
                                                    overflow: TextOverflow.ellipsis,
                                                  ),
                                                  const SizedBox(height: 4),
                                                  Wrap(
                                                    spacing: 8,
                                                    runSpacing: 8,
                                                    children: [
                                                      _buildInfoChip(
                                                        icon: Icons.wifi,
                                                        label: printer.ip ?? 'N/A',
                                                      ),
                                                      _buildInfoChip(
                                                        icon: Icons.link,
                                                        label: printer.mac ?? 'N/A',
                                                      ),
                                                    ],
                                                  ),
                                                ],
                                              ),
                                            ),
                                            const SizedBox(width: 8),
                                            if (isSelected)
                                              const Icon(
                                                Icons.check_circle,
                                                color: Color(0xFF5C4FE1),
                                                size: 24,
                                              ),
                                          ],
                                        ),
                                      ),
                                    ),
                                  );
                                },
                              ),
                      ),
                    ],
                  ),
                ),
              ),
            );
          },
        );
      },
    ).whenComplete(() {
      _dialogSetState = null;
    });
  }

  Widget _buildInfoChip({
    required IconData icon,
    required String label,
  }) {
    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: 4,
        vertical: 4,
      ),
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.1),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(
            icon,
            size: 8,
            color: Colors.white.withOpacity(0.7),
          ),
          const SizedBox(width: 4),
          Flexible(
            child: Text(
              label,
              style: TextStyle(
                color: Colors.white.withOpacity(0.7),
                fontSize: 12,
              ),
              overflow: TextOverflow.ellipsis,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSectionHeader(String title) {
    return Row(
      children: [
        const Icon(Icons.arrow_back),
        const SizedBox(width: 16),
        Text(
          title,
          style: const TextStyle(
            fontSize: 24,
            fontWeight: FontWeight.bold,
            color: Colors.white,
          ),
        ),
      ],
    );
  }

  Widget _buildSection({
    required String title,
    required List<Widget> children,
  }) {
    return Container(
      decoration: BoxDecoration(
        color: const Color(0xFF2A2B2E),
        borderRadius: BorderRadius.circular(16),
      ),
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          _buildSectionHeader(title),
          const SizedBox(height: 16),
          ...children,
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF1A1B1E),
      appBar: AppBar(
        backgroundColor: const Color(0xFF1A1B1E),
        elevation: 0,
        title: const Text(
          'THERMIS',
          style: TextStyle(
            fontSize: 28,
            fontWeight: FontWeight.bold,
            color: Colors.white,
          ),
        ),
        actions: [
          IconButton(
            icon: const Icon(
              Icons.settings_outlined,
              color: Colors.white,
            ),
            onPressed: () {
              // Add settings functionality here
            },
          ),
        ],
      ),
      body: SingleChildScrollView(
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const SizedBox(height: 24),
              ElevatedButton.icon(
                onPressed: () async {
                  final receiptDTOJSON = await rootBundle.loadString('assets/customer.json');
                  final imageBytes = await Thermis.getReceiptReview(receiptDTOJSON);
                  if (imageBytes != null && context.mounted) {
                    await _showPreviewDialog(context, imageBytes);
                  }
                },
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFF5C4FE1),
                  padding: const EdgeInsets.symmetric(vertical: 12),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(16),
                  ),
                ),
                icon: const Icon(Icons.receipt_outlined, size: 24),
                label: const Text(
                  'Receipt Preview',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ),
              const SizedBox(height: 24),
              _buildSection(
                title: 'USB Printer',
                children: [
                  Container(
                    decoration: BoxDecoration(
                      color: const Color(0xFF1A1B1E),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    padding: const EdgeInsets.all(16),
                    child: const Text(
                      'Ready to print. Press the button below to start a test print.',
                      style: TextStyle(
                        fontSize: 16,
                        color: Colors.white70,
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),
                  ElevatedButton.icon(
                    onPressed: () async {
                      final receiptDTOJSON = await rootBundle.loadString('assets/customer.json');
                      await Thermis.printReceipt(receiptDTOJSON);
                    },
                    icon: const Icon(Icons.print),
                    label: const Text('Test Print'),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              _buildSection(
                title: 'LAN Printer',
                children: [
                  Container(
                    decoration: BoxDecoration(
                      color: const Color(0xFF1A1B1E),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          'Selected Printer:',
                          style: TextStyle(
                            fontSize: 14,
                            color: Colors.white70,
                          ),
                        ),
                        const SizedBox(height: 8),
                        Text(
                          selectedPrinter,
                          style: const TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                            color: Colors.white,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 16),
                  ElevatedButton.icon(
                    onPressed: () {
                      _startDiscovery();
                    },
                    icon: const Icon(Icons.search),
                    label: const Text('Discover LAN Printers'),
                  ),
                  const SizedBox(height: 16),
                  ElevatedButton.icon(
                    onPressed: _testParallelPrinting,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: const Color(0xFF4CAF50),
                    ),
                    icon: const Icon(Icons.devices),
                    label: const Text('Test Parallel Printing'),
                  ),
                  const SizedBox(height: 8),
                  ElevatedButton.icon(
                    onPressed: _checkDeviceQueues,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: const Color(0xFF2196F3),
                    ),
                    icon: const Icon(Icons.queue_outlined),
                    label: const Text('Check Device Queues'),
                  ),
                  const SizedBox(height: 16),
                  ElevatedButton.icon(
                    onPressed: () async {
                      final receiptDTOJSON = await rootBundle.loadString('assets/kitchen.json');
                      // Print to multiple devices simultaneously
                      Thermis.printReceipt(receiptDTOJSON, config: PrinterConfig(
                        printerType: PrinterType.starMCLan,
                        macAddresses: selectedPrinter == '-' 
                          ? [] // Demo multiple MACs
                          : [selectedPrinter, selectedPrinter, selectedPrinter],
                      ));

                      Thermis.printReceipt(receiptDTOJSON, config: PrinterConfig(
                        printerType: PrinterType.starMCLan,
                        macAddresses: selectedPrinter == '-'
                            ? [] // Demo multiple MACs
                            : [selectedPrinter, selectedPrinter, selectedPrinter],
                      ));
                    },
                    icon: const Icon(Icons.print),
                    label: Text(selectedPrinter == '-' 
                      ? 'Test Print (Multiple Devices)' 
                      : 'Test Print (Selected Device)'),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _testParallelPrinting() async {
    try {
      print('Starting parallel print test...');
      
      // Test parallel printing to different devices
      final usbConfig = PrinterConfig(printerType: PrinterType.usbGeneric);
      final lan1Config = PrinterConfig(
        printerType: PrinterType.starMCLan,
        macAddresses: [selectedPrinter],
      );
      final lan2Config = PrinterConfig(
        printerType: PrinterType.starMCLan,
        macAddresses: ['11:22:33:44:55:66'],
      );

      // These will execute in parallel (different device queues)
      final futures = [
        Thermis.printReceipt('{"test": "USB printer"}', config: usbConfig),
        Thermis.printReceipt('{"test": "LAN printer 1"}', config: lan1Config),
        Thermis.printReceipt('{"test": "LAN printer 2"}', config: lan2Config),
      ];

      final results = await Future.wait(futures);
      print('Parallel Print Results: $results');
    } catch (e) {
      print('Parallel Print Error: $e');
    }
  }

  Future<void> _checkDeviceQueues() async {
    try {
      final deviceQueues = await Thermis.getDeviceQueueSizes();
      final totalQueue = await Thermis.getQueueSize();
      print('Total Queue: $totalQueue');
      print('Device Queues: $deviceQueues');
    } catch (e) {
      print('Queue Check Error: $e');
    }
  }
}