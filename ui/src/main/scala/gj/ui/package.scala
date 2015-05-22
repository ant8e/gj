package gj

package object ui {
  sealed trait TabId
  object TabId {
    object DashBoardTabId extends TabId
    object SettingsTabId extends TabId
  }
}
