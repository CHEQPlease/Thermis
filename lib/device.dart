class Device {
  final String deviceName;
  final String ip;
  final String mac;

  Device({
    required this.deviceName,
    required this.ip,
    required this.mac,
  });

  factory Device.fromMap(Map<String, dynamic> map) {
    return Device(
      deviceName: map['deviceName'] as String,
      ip: map['ip'] as String,
      mac: map['mac'] as String,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'deviceName': deviceName,
      'ip': ip,
      'mac': mac,
    };
  }
} 