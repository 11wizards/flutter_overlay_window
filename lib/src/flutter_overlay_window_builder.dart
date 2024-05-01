import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_overlay_window/flutter_overlay_window.dart';

class FlutterOverlayWindowBuilder<T extends Object> extends StatefulWidget {
  final Widget Function(BuildContext context, T? data) builder;
  final T? Function(String data) decoder;

  const FlutterOverlayWindowBuilder({
    super.key,
    required this.builder,
    required this.decoder,
  });

  @override
  State<FlutterOverlayWindowBuilder<T>> createState() =>
      _FlutterOverlayWindowBuilderState<T>();
}

class _FlutterOverlayWindowBuilderState<T extends Object>
    extends State<FlutterOverlayWindowBuilder<T>> {
  late StreamSubscription _overlayDataSubscription;
  T? _overlayData;

  @override
  void initState() {
    super.initState();

    _overlayDataSubscription =
        FlutterOverlayWindow.attachOverlay().listen((overlayData) {
      setState(() {
        _overlayData = widget.decoder(overlayData!);
      });
    });
  }

  @override
  void dispose() {
    _overlayDataSubscription.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => widget.builder(context, _overlayData);
}
