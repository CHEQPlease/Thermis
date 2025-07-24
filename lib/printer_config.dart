enum PrinterType {
  usbGeneric,
  starMCLan,
}

class PrinterConfig {
  final PrinterType printerType;
  final List<String>? macAddresses;

  const PrinterConfig({
    required this.printerType,
    this.macAddresses,
  });

  Map<String, dynamic> toMap() {
    return {
      'printer_type': printerType.name,
      'mac_addresses': macAddresses,
    };
  }
}
