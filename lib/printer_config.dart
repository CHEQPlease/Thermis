enum PrinterType {
  usbGeneric,
  starMCLan,
}

class PrinterConfig {
  final PrinterType printerType;
  final List<String>? macAddresses;

  PrinterConfig({
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

/// Represents the result of a print operation
class PrintResult {
  final bool success;
  final PrintFailureReason? reason;
  final bool retryable;
  final String? message;

  PrintResult({
    required this.success,
    this.reason,
    required this.retryable,
    this.message,
  });

  factory PrintResult.fromMap(Map<String, dynamic> map) {
    PrintFailureReason? reason;
    if (map['reason'] != null) {
      final reasonString = map['reason'].toString();
      // Try to find by camelCase name first
      reason = PrintFailureReason.values.firstWhere(
        (e) => e.name == reasonString,
        orElse: () {
          // If not found, try to find by UPPER_CASE conversion
          final upperCaseToEnum = {
            'PRINTER_BUSY': PrintFailureReason.printerBusy,
            'PRINTER_OFFLINE': PrintFailureReason.printerOffline,
            'PRINTER_NOT_FOUND': PrintFailureReason.printerNotFound,
            'OUT_OF_PAPER': PrintFailureReason.outOfPaper,
            'COVER_OPEN': PrintFailureReason.coverOpen,
            'NETWORK_ERROR': PrintFailureReason.networkError,
            'COMMUNICATION_ERROR': PrintFailureReason.communicationError,
            'DEVICE_IN_USE': PrintFailureReason.deviceInUse,
            'TIMEOUT_ERROR': PrintFailureReason.timeoutError,
            'UNKNOWN_ERROR': PrintFailureReason.unknownError,
          };
          return upperCaseToEnum[reasonString] ?? PrintFailureReason.unknownError;
        },
      );
    }

    return PrintResult(
      success: map['success'] is bool ? map['success'] : (map['success'].toString().toLowerCase() == 'true'),
      reason: reason,
      retryable: map['retryable'] is bool ? map['retryable'] : (map['retryable'].toString().toLowerCase() == 'true'),
      message: map['message']?.toString(),
    );
  }

  @override
  String toString() {
    if (success) return 'PrintResult.Success';
    return 'PrintResult.Failed(reason: $reason, retryable: $retryable, message: $message)';
  }
}

/// Enum representing different types of print failures
enum PrintFailureReason {
  printerBusy,
  printerOffline,
  printerNotFound,
  outOfPaper,
  coverOpen,
  networkError,
  communicationError,
  deviceInUse,
  timeoutError,
  unknownError;

  bool get isRetryable {
    switch (this) {
      case PrintFailureReason.printerBusy:
      case PrintFailureReason.deviceInUse:
      case PrintFailureReason.networkError:
      case PrintFailureReason.communicationError:
      case PrintFailureReason.timeoutError:
        return true;
      case PrintFailureReason.printerOffline:
      case PrintFailureReason.printerNotFound:
      case PrintFailureReason.outOfPaper:
      case PrintFailureReason.coverOpen:
      case PrintFailureReason.unknownError:
        return false;
    }
  }

  String get displayName {
    switch (this) {
      case PrintFailureReason.printerBusy:
        return 'Printer is busy';
      case PrintFailureReason.printerOffline:
        return 'Printer is offline';
      case PrintFailureReason.printerNotFound:
        return 'Printer not found';
      case PrintFailureReason.outOfPaper:
        return 'Out of paper';
      case PrintFailureReason.coverOpen:
        return 'Printer cover is open';
      case PrintFailureReason.networkError:
        return 'Network error';
      case PrintFailureReason.communicationError:
        return 'Communication error';
      case PrintFailureReason.deviceInUse:
        return 'Device is in use';
      case PrintFailureReason.timeoutError:
        return 'Operation timed out';
      case PrintFailureReason.unknownError:
        return 'Unknown error';
    }
  }
}
