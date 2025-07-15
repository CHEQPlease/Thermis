class PrinterConfig {
  final String printerType;
  final String printerMAC;

  PrinterConfig({
    required this.printerType,
    required this.printerMAC,
  });

  @override
  String toString() {
    return 'PrinterConfig(printerType: $printerType, printerMAC: $printerMAC)';
  }

  Map<String, String> toMap() {
    return {
      'printer_type': printerType,
      'printer_mac': printerMAC,
    };
  }
}
