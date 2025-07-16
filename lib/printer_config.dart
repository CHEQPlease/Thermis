class PrinterConfig {
  final PrinterType printerType;
  final String? printerMAC;

  PrinterConfig({
    required this.printerType,
    this.printerMAC,
  });

  @override
  String toString() {
    return 'PrinterConfig(printerType: $printerType, printerMAC: $printerMAC)';
  }

  Map<String, String?> toMap() {
    return {
      'printer_type': printerType.name,
      'printer_mac': printerMAC,
    };
  }
}

enum PrinterType {
  generic,
  starmc,
}
