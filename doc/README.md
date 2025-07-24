# Thermis Documentation

Welcome to the comprehensive documentation for the Thermis Flutter plugin. This documentation provides everything you need to integrate thermal receipt printing into your Flutter applications.

## 📚 Documentation Structure

### 🚀 [Main README](../README.md)
The primary documentation file containing:
- Quick start guide
- Installation instructions
- Basic usage examples
- Feature overview
- Troubleshooting guide

### 📖 [API Reference](API_REFERENCE.md)
Complete reference for all public APIs:
- Method signatures and parameters
- Return types and error handling
- Detailed usage examples
- Data models and enums
- Error handling patterns

### 💡 [Examples & Patterns](EXAMPLES.md)
Real-world usage examples and patterns:
- Basic and advanced printing scenarios
- Device discovery implementations
- Queue management strategies
- Error handling patterns
- Production integration examples

### 🧪 [Test Documentation](../test/README.md)
Comprehensive testing information:
- Test suite overview (94 tests)
- Test coverage details
- Running tests locally
- Writing new tests
- Mock implementations

### 📝 [Changelog](../CHANGELOG.md)
Version history and release notes:
- Feature additions and improvements
- Breaking changes and migrations
- Bug fixes and performance improvements
- Future roadmap

## 🎯 Quick Navigation

### For New Users
1. Start with the [Main README](../README.md) for installation and basic usage
2. Review the [Quick Start](../README.md#-quick-start) section
3. Explore [Basic Examples](EXAMPLES.md#basic-usage)
4. Check [Troubleshooting](../README.md#-troubleshooting) for common issues

### For Developers
1. Review the [API Reference](API_REFERENCE.md) for detailed method documentation
2. Study [Advanced Examples](EXAMPLES.md#advanced-printing) for complex scenarios
3. Examine the [Test Suite](../test/README.md) for implementation patterns
4. Check [Error Handling](API_REFERENCE.md#error-handling) for robust implementations

### For Integration
1. Review [Production Patterns](EXAMPLES.md#production-patterns) for real-world usage
2. Study [Queue Management](EXAMPLES.md#queue-management) for high-volume scenarios
3. Implement [Error Recovery](EXAMPLES.md#error-handling) for reliability
4. Follow [Best Practices](API_REFERENCE.md#best-practices) for optimal performance

## 🔍 Key Features Covered

### Core Functionality
- **Receipt Printing**: Print thermal receipts with detailed formatting
- **Multi-Printer Support**: USB Generic and Star Micronics LAN printers
- **Device Discovery**: Automatic printer detection and configuration
- **Queue Management**: Advanced job queuing with per-device separation

### Advanced Features
- **Error Handling**: Comprehensive error classification and retry logic
- **Async Operations**: Non-blocking operations with detailed result feedback
- **Configuration Management**: Flexible printer configuration options
- **Performance Optimization**: Efficient resource usage and parallel processing

### Developer Tools
- **Comprehensive Testing**: 94 test cases covering all functionality
- **Type Safety**: Full null safety and strong typing support
- **Debug Support**: Detailed logging and error reporting
- **Hot Reload**: Full Flutter development workflow support

## 📋 Common Use Cases

### Restaurant POS Systems
- Kitchen order printing
- Customer receipt generation
- Cash drawer integration
- End-of-day reporting

### Retail Applications
- Sales receipts
- Return receipts
- Inventory labels
- Promotional materials

### Service Industries
- Service tickets
- Appointment confirmations
- Payment receipts
- Customer notifications

## 🛠️ Development Workflow

### Setup & Configuration
1. **Installation**: Add plugin to `pubspec.yaml`
2. **Permissions**: Configure Android permissions for USB/Network
3. **Discovery**: Implement printer discovery for your environment
4. **Testing**: Set up test printers and validate functionality

### Implementation
1. **Basic Integration**: Start with simple receipt printing
2. **Error Handling**: Implement comprehensive error handling
3. **Queue Management**: Add queue monitoring for production use
4. **Performance**: Optimize for your specific use case

### Production Deployment
1. **Testing**: Run comprehensive test suite
2. **Error Recovery**: Implement retry and recovery logic
3. **Monitoring**: Add queue and performance monitoring
4. **Documentation**: Document your specific configuration

## 🔧 Configuration Examples

### USB Printer Setup
```dart
final usbConfig = PrinterConfig(
  printerType: PrinterType.usbGeneric,
);

final result = await Thermis.printReceipt(receiptJson, config: usbConfig);
```

### LAN Printer Setup
```dart
final lanConfig = PrinterConfig(
  printerType: PrinterType.starMCLan,
  macAddresses: ['AA:BB:CC:DD:EE:FF'],
);

final result = await Thermis.printReceipt(receiptJson, config: lanConfig);
```

### Multi-Printer Setup
```dart
final multiConfig = PrinterConfig(
  printerType: PrinterType.starMCLan,
  macAddresses: [
    'AA:BB:CC:DD:EE:FF', // Kitchen
    '11:22:33:44:55:66', // Counter
    '99:88:77:66:55:44', // Office
  ],
);
```

## 📊 Performance Guidelines

### Queue Management
- Monitor queue sizes regularly
- Clear queues when they exceed reasonable limits
- Use per-device queues for parallel processing
- Implement queue health monitoring

### Error Handling
- Always check `PrintResult` for operation status
- Implement retry logic for retryable errors
- Provide user feedback for non-retryable errors
- Log errors for debugging and monitoring

### Resource Management
- Properly dispose of discovery streams
- Clean up resources on app termination
- Monitor memory usage in high-volume scenarios
- Implement connection pooling for network printers

## 🆘 Getting Help

### Documentation Issues
If you find issues with the documentation:
1. Check the [GitHub Issues](https://github.com/CHEQPlease/Thermis/issues)
2. Search for existing documentation issues
3. Create a new issue with the "documentation" label

### Technical Support
For technical issues:
1. Review the [Troubleshooting Guide](../README.md#-troubleshooting)
2. Check the [API Reference](API_REFERENCE.md) for detailed information
3. Examine [Examples](EXAMPLES.md) for similar use cases
4. Create a GitHub issue with detailed information

### Contributing
To contribute to the documentation:
1. Fork the repository
2. Make your improvements
3. Test your changes
4. Submit a pull request

## 📈 Version Information

- **Current Version**: 1.6.0
- **Minimum Flutter**: 3.0+
- **Minimum Dart**: 3.0+
- **Supported Platforms**: Android (API 21+)

For version history and migration guides, see the [Changelog](../CHANGELOG.md).

---

**Happy Printing! 🖨️**

*This documentation is maintained by the CHEQ team and the open-source community.* 