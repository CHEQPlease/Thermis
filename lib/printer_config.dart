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
    return PrintResult(
      success: map['success'] ?? false,
      reason: map['reason'] != null 
          ? PrintFailureReason.values.firstWhere(
              (e) => e.name == map['reason'],
              orElse: () => PrintFailureReason.unknownError,
            )
          : null,
      retryable: map['retryable'] ?? false,
      message: map['message'],
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
