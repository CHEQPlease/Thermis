import 'package:flutter_test/flutter_test.dart';
import 'package:thermis/thermis.dart';
import 'package:thermis/thermis_platform_interface.dart';
import 'package:thermis/thermis_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockThermisPlatform
    with MockPlatformInterfaceMixin
    implements ThermisPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final ThermisPlatform initialPlatform = ThermisPlatform.instance;

  test('$MethodChannelThermis is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelThermis>());
  });

  test('getPlatformVersion', () async {
    Thermis thermisPlugin = Thermis();
    MockThermisPlatform fakePlatform = MockThermisPlatform();
    ThermisPlatform.instance = fakePlatform;

    expect(await thermisPlugin.getPlatformVersion(), '42');
  });
}
