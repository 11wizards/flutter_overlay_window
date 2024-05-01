import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_overlay_window/src/overlay_config.dart';
import 'package:rxdart/rxdart.dart';

class FlutterOverlayWindow {
  FlutterOverlayWindow._();

  static const MethodChannel _channel =
      MethodChannel("x-slayer/overlay_channel");
  static const MethodChannel _overlayChannel =
      MethodChannel("x-slayer/overlay");

  static const BasicMessageChannel _overlayMessageChannel = BasicMessageChannel(
    "x-slayer/overlay_messenger",
    StandardMessageCodec(),
  );

  static final ReplaySubject<String?> _overlayMessageChannelDataStream =
      ReplaySubject<String?>(maxSize: 1);

  static bool _listeningToMessageChannel = false;

  /// Open overlay content
  ///
  /// - Optional arguments:
  /// `height` the overlay height and default is [WindowSize.fullCover]
  /// `width` the overlay width and default is [WindowSize.matchParent]
  /// `alignment` the alignment position on screen and default is [OverlayAlignment.center]
  /// `visibilitySecret` the detail displayed in notifications on the lock screen and default is [NotificationVisibility.visibilitySecret]
  /// `OverlayFlag` the overlay flag and default is [OverlayFlag.defaultFlag]
  /// `overlayTitle` the notification message and default is "overlay activated"
  /// `overlayContent` the notification message
  /// `enableDrag` to enable/disable dragging the overlay over the screen and default is "false"
  /// `positionGravity` the overlay position after drag and default is [PositionGravity.none]
  static Future<void> showOverlay({
    int height = WindowSize.fullCover,
    int width = WindowSize.matchParent,
    int? minimumVisibleWidth,
    int? minimumVisibleHeight,
    String overlayTitle = "overlay activated",
    String? overlayContent,
    String? data,
  }) =>
      _channel.invokeMethod(
        'showOverlay',
        {
          "height": height,
          "width": width,
          "minimumVisibleWidth": minimumVisibleWidth,
          "minimumVisibleHeight": minimumVisibleHeight,
          "overlayTitle": overlayTitle,
          "overlayContent": overlayContent,
          "data": data,
        },
      );

  /// Closes overlay if open
  static Future<bool?> closeOverlay() => _channel.invokeMethod('closeOverlay');

  /// Update the overlay size in the screen
  static Future<bool?> resizeOverlay(
    int width,
    int height, {
    int? minimumVisibleWidth,
    int? minimumVisibleHeight,
  }) =>
      _overlayChannel.invokeMethod<bool?>(
        'resizeOverlay',
        {
          'width': width,
          'height': height,
          'minimumVisibleWidth': minimumVisibleWidth,
          'minimumVisibleHeight': minimumVisibleHeight,
        },
      );

  /// Returns a Stream of overlay data. When listened to it will emit the last
  /// value received.
  static Stream<String?> attachOverlay() {
    if (!_listeningToMessageChannel) {
      _listeningToMessageChannel = true;
      _overlayMessageChannel.setMessageHandler(
        (message) async => _overlayMessageChannelDataStream.add(message),
      );
    }
    _overlayMessageChannel.send('attachOverlay');
    return _overlayMessageChannelDataStream.stream;
  }

  /// Check if the current overlay is active
  static Future<bool> isActive() async =>
      (await _channel.invokeMethod<bool?>('isOverlayActive')) ?? false;
}
