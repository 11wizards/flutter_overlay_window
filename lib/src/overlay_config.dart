enum OverlayFlag {
  /// Window flag: this window can never receive touch events.
  /// Usefull if you want to display click-through overlay
  @Deprecated('Use "clickThrough" instead.')
  flagNotTouchable,

  /// Window flag: this window won't ever get key input focus
  /// so the user can not send key or other button events to it.
  @Deprecated('Use "defaultFlag" instead.')
  flagNotFocusable,

  /// Window flag: allow any pointer events outside of the window to be sent to the windows behind it.
  /// Usefull when you want to use fields that show keyboards.
  @Deprecated('Use "focusPointer" instead.')
  flagNotTouchModal,

  /// Window flag: this window can never receive touch events.
  /// Usefull if you want to display click-through overlay
  clickThrough,

  /// Window flag: this window won't ever get key input focus
  /// so the user can not send key or other button events to it.
  defaultFlag,

  /// Window flag: allow any pointer events outside of the window to be sent to the windows behind it.
  /// Usefull when you want to use fields that show keyboards.
  focusPointer,
}

/// The level of detail displayed in notifications on the lock screen.
enum NotificationVisibility {
  /// Show this notification in its entirety on all lockscreens.
  visibilityPublic,

  /// Do not reveal any part of this notification on a secure lockscreen.
  visibilitySecret,

  /// Show this notification on all lockscreens, but conceal sensitive or private information on secure lockscreens.
  visibilityPrivate
}

class WindowSize {
  WindowSize._();

  /// default size when the overlay match the parent size
  /// basically it will take the full screen width and height
  static const int matchParent = -1;

  /// make the overlay cover the fullscreen
  /// even the statusbar and the navigationbar
  static const int fullCover = -1999;
}
